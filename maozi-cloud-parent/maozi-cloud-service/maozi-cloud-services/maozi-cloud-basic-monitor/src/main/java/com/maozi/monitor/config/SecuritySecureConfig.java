package com.maozi.monitor.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Spring Boot Admin 监控仪表盘安全配置
 * <p>
 * 启用基于表单登录的认证：静态资源、健康检查与登录页匿名可访问，其余请求需认证；
 * 同时提供 HTTP Basic、CSRF 保护（Cookie 存储 + SPA 明文 token 适配）、
 * Remember-Me（有效期 14 天）与内存用户认证。
 * 登录用户名密码来自 {@link SecurityProperties}（spring.security.user.* 配置项）。
 * </p>
 *
 * @author maozi
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecuritySecureConfig {

	/** Spring Boot Admin 服务端属性 */
	private final AdminServerProperties adminServer;

	/** Spring Security 安全属性（包含默认用户名密码） */
	private final SecurityProperties security;

	/**
	 * 构造方法，注入 Admin Server 属性与安全属性配置。
	 *
	 * @param adminServer Spring Boot Admin 服务端属性（提供监控台路径前缀）
	 * @param security    Spring Security 安全属性（含监控台登录用户名密码）
	 */
	public SecuritySecureConfig(AdminServerProperties adminServer, SecurityProperties security) {
		this.adminServer = adminServer;
		this.security = security;
	}

	/**
	 * 安全过滤链：配置认证授权、表单登录、CSRF、Remember-Me 等。
	 *
	 * @param http HttpSecurity对象，用于构建监控台的安全过滤器链
	 * @return 配置完成的监控台安全过滤器链
	 * @throws Exception 配置过程中可能抛出的异常
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// 登录成功后重定向处理器
		SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
		successHandler.setTargetUrlParameter("redirectTo");
		// 首页 UiController 的映射经前缀化后是去尾斜杠的 "/monitor" (Spring 6
		// PathPattern 不再将 "/monitor/" 归一到 "/monitor"), 而 adminServer.path("/")
		// 在配置了 context-path 时返回 "/monitor/" —— 直接作默认落点会 404, 需去掉尾斜杠
		String defaultTargetUrl = this.adminServer.path("/");
		if (!"/".equals(defaultTargetUrl)) {
			defaultTargetUrl = StringUtils.trimTrailingCharacter(defaultTargetUrl, '/');
		}
		successHandler.setDefaultTargetUrl(defaultTargetUrl);

		// Spring Security 6.1+ 默认使用 XorCsrfTokenRequestAttributeHandler（BREACH 保护），
		// 会将表单里的 _csrf 做 XOR 编码；而 SBA 的 Vue 前端是从 XSRF-TOKEN cookie 读明文 token 放到请求 header，
		// 明文 token 会被 Xor 处理器判为不匹配 → 403。
		// 这里改用 CsrfTokenRequestAttributeHandler 做明文匹配，适配 SPA 的 cookie-header 模式。
		CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
		csrfRequestHandler.setCsrfRequestAttributeName(null);

		http
				// Spring Security 6.0+ 默认让整条安全过滤链对 ASYNC / ERROR dispatch 也生效，会引发两个问题：
				// 1. ASYNC：SBA 的 /applications 是 SSE 端点，初始 REQUEST 认证通过后开始推流（response 已 committed），
				//    后续 ASYNC dispatch 再进入安全链时，异步线程拿不到 SecurityContext，RememberMeAuthenticationFilter
				//    误判未登录 → 重新认证 → 想 getSession() 存上下文 → 但响应已提交 → Undertow 抛 UT010067。
				// 2. ERROR：SSE 推流出错会触发 ERROR dispatch 到 /error，安全链判未登录想重定向到登录页，
				//    但响应已提交无法重定向 → 输出 status:999，浏览器跳到 /error?continue。
				// 解法：securityMatcher 让整条安全链只处理初始 REQUEST，ASYNC / ERROR dispatch 全部跳过。
				// 初始 REQUEST 已完成认证，异步推流和错误分发阶段无需再跑安全过滤器。
				.securityMatcher((RequestMatcher) request -> {
					DispatcherType type = request.getDispatcherType();
					return type != DispatcherType.ASYNC && type != DispatcherType.ERROR;
				})
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(this.adminServer.path("/assets/**")).permitAll()
						// SBA 在根路径动态生成的两个运行时资源（不在 /assets/ 下），登录页 HTML 就引用它们：
						// /sba-settings.js 是前端运行时配置，/variables.css 是主题变量。
						// 必须匿名可访问，否则会被重定向到登录页成为 saved request，登录成功后 successHandler
						// 把用户重定向到这些文件，浏览器直接展示 JS/CSS 文本而非进入主页。
						.requestMatchers(this.adminServer.path("/sba-settings.js")).permitAll()
						.requestMatchers(this.adminServer.path("/variables.css")).permitAll()
						.requestMatchers(this.adminServer.path("/actuator/**")).permitAll()
						.requestMatchers(this.adminServer.path("/login")).permitAll()
						.anyRequest().authenticated())
				.formLogin((formLogin) -> formLogin
						.loginPage(this.adminServer.path("/login"))
						.successHandler(successHandler))
				.logout((logout) -> logout
						.logoutUrl(this.adminServer.path("/logout"))
						.logoutSuccessUrl(this.adminServer.path("/login?logout")))
				.httpBasic(Customizer.withDefaults())
				// 关闭 X-Frame-Options：管理后台"中间件管理"页以内嵌 iframe 打开监控台，
				// Spring Security 默认下发的 DENY 会导致浏览器拒绝在 frame 中展示
				.headers((headers) -> headers.frameOptions((frame) -> frame.disable()))
				.csrf((csrf) -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(csrfRequestHandler)
						.ignoringRequestMatchers(
								new AntPathRequestMatcher(this.adminServer.path("/instances"), HttpMethod.POST.toString()),
								new AntPathRequestMatcher(this.adminServer.path("/instances/*"), HttpMethod.DELETE.toString()),
								new AntPathRequestMatcher(this.adminServer.path("/actuator/**")),
								new AntPathRequestMatcher(this.adminServer.path("/logout"))))
				.rememberMe((rememberMe) -> rememberMe
						.key(UUID.randomUUID().toString())
						.tokenValiditySeconds(1209600));

		return http.build();
	}

	/**
	 * 内存用户详情服务：为 Remember-Me 及表单登录提供用户认证信息。
	 * <p>
	 * 密码使用 {noop} 前缀，表示明文存储（不经过 PasswordEncoder 加密），
	 * 由 Spring Security 的 DelegatingPasswordEncoder 识别。
	 * </p>
	 *
	 * @return 基于配置用户的内存 UserDetailsService
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		UserDetails user = User.builder()
				.username(security.getUser().getName())
				.password("{noop}" + security.getUser().getPassword())
				.roles("USER")
				.build();
		return new InMemoryUserDetailsManager(user);
	}

}
