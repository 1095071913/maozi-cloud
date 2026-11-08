/** 书签公共类型 */

export interface Bookmark {
  /** 唯一标识（UUID） */
  id: string
  /** 页面地址 */
  url: string
  /** 页面标题 */
  title: string
  /** 页面描述 */
  description: string
  /** 网页图标（dataURL，自包含存储） */
  icon?: string
  /** 登录账号 */
  username: string
  /** 登录密码（存储时经系统钥匙串加密，内存中明文） */
  password: string
  /** 三层分类：归宿（如 xx 公司） */
  category1: string
  /** 三层分类：环境（如 开发环境） */
  category2: string
  /** 三层分类：网页类型（如 运维类型） */
  category3: string
  /** 排序序号（拖拽排序后持久化，旧数据缺省按创建时间排） */
  sortOrder?: number
  /** 创建时间戳（毫秒） */
  createdAt: number
}

/** 自动抓取的网页信息 */
export interface PageMeta {
  /** 网页标题 */
  title?: string
  /** 网页描述 */
  description?: string
  /** dataURL */
  icon?: string
}
