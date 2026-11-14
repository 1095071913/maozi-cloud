package com.maozi.ai.ai.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话消息存储结构
 * <p>
 * 消息对象序列化为 JSON 存入对话记忆（Redis）时的载体结构，
 * 统一承载各类型消息（系统/用户/AI/工具）的属性：
 * 消息类型、元数据、媒体、工具调用、工具响应、文本内容等。
 * 媒体字段使用自定义序列化器保证可无损往返。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/17 03:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** Media JSON 字段名：MIME 类型对象 */
    private static final String FIELD_MIME_TYPE = "mimeType";

    /** Media JSON 字段名：MIME 主类型 */
    private static final String FIELD_TYPE = "type";

    /** Media JSON 字段名：MIME 子类型 */
    private static final String FIELD_SUBTYPE = "subtype";

    /** Media JSON 字段名：MIME 参数表 */
    private static final String FIELD_PARAMETERS = "parameters";

    /** Media JSON 字段名：媒体数据（byte[] 数组或 URI 字符串） */
    private static final String FIELD_DATA = "data";

    /** Media JSON 字段名：媒体 ID */
    private static final String FIELD_ID = "id";

    /** Media JSON 字段名：媒体名称 */
    private static final String FIELD_NAME = "name";

    /** 消息类型（SYSTEM / USER / ASSISTANT / TOOL 等） */
    private String messageType;

    /** 消息元数据（包含消息 ID、写入时间戳等，见 ChatMessageConstant） */
    private Map<String,Object> metadata = Map.of();

    /** 消息携带的媒体列表（多模态图片等），使用自定义序列化器保证无损往返 */
    @JsonSerialize(contentUsing = MediaSerializer.class)
    @JsonDeserialize(contentUsing = MediaDeserializer.class)
    private List<Media> media = List.of();

    /** AI 消息发起的工具调用列表 */
    private List<AssistantMessage.ToolCall> toolCalls = List.of();

    /** 消息文本内容 */
    private String textContent;

    /** 工具响应消息的工具响应列表 */
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of();

    /** 附加参数 */
    private Map<String, Object> params = Map.of();

    /**
     * Media 反序列化器
     * <p>
     * {@link Media} 为不可变对象，无默认构造器，Jackson 无法自动反序列化，
     * 需解析 JSON 后通过 Builder 手动构建。data 字段兼容两种形态：
     * byte[]（序列化为整型数组）与 URI（序列化为字符串）。
     * </p>
     *
    * @author maozi
     */
    public static class MediaDeserializer extends JsonDeserializer<Media> {

        /**
         * 从 JSON 节点还原 Media 对象
         *
         * @param parser  JSON 解析器，指向当前 Media 节点
         * @param context 反序列化上下文，当前未使用
         * @return 还原后的 Media 对象
         * @throws IOException 读取 JSON 树失败时抛出
         */
        @Override
        public Media deserialize(JsonParser parser, DeserializationContext context) throws IOException {

            JsonNode node = parser.getCodec().readTree(parser);

            // 解析 MIME 类型节点及其携带的参数（字段缺失时视为脏数据快速失败，避免 NPE）
            JsonNode mimeTypeNode = node.get(FIELD_MIME_TYPE);
            if (mimeTypeNode == null || mimeTypeNode.isNull()) {
                throw new IOException("Media JSON 缺少 mimeType 字段");
            }
            JsonNode typeNode = mimeTypeNode.get(FIELD_TYPE);
            JsonNode subtypeNode = mimeTypeNode.get(FIELD_SUBTYPE);
            if (typeNode == null || typeNode.isNull() || subtypeNode == null || subtypeNode.isNull()) {
                throw new IOException("Media JSON 缺少 MIME 主/子类型字段");
            }

            Map<String, String> parameters = new LinkedHashMap<>(8);
            JsonNode parametersNode = mimeTypeNode.get(FIELD_PARAMETERS);
            if (parametersNode != null) {
                parametersNode.fieldNames().forEachRemaining(name -> parameters.put(name, parametersNode.get(name).asText()));
            }

            Media.Builder builder = Media.builder()
                    .mimeType(new MimeType(typeNode.asText(), subtypeNode.asText(), parameters));

            // data 为整型数组（byte[]）时按字节还原，否则按 URI 字符串处理
            JsonNode dataNode = node.get(FIELD_DATA);
            if (dataNode == null || dataNode.isNull()) {
                throw new IOException("Media JSON 缺少 data 字段");
            }
            if (dataNode.isArray()) {
                byte[] data = new byte[dataNode.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) dataNode.get(i).asInt();
                }
                builder.data(data);
            } else {
                builder.data(dataNode.asText());
            }

            // id 与 name 为可选字段，序列化时可能不存在，存在时才回填
            if (node.hasNonNull(FIELD_ID)) {
                builder.id(node.get(FIELD_ID).asText());
            }

            if (node.hasNonNull(FIELD_NAME)) {
                builder.name(node.get(FIELD_NAME).asText());
            }

            return builder.build();

        }

    }

    /**
     * Media 序列化器
     * <p>
     * 与 {@link MediaDeserializer} 对称，序列化为约定的 JSON 结构：
     * data 为 byte[] 时输出整型数组（与 hutool 序列化格式一致，而非 Jackson 默认的 base64 字符串），
     * 为 URI 时输出字符串，保证序列化与反序列化可无损往返。
     * </p>
     *
    * @author maozi
     */
    public static class MediaSerializer extends JsonSerializer<Media> {

        /**
         * 将 Media 对象写入约定的 JSON 结构
         *
         * @param media     待序列化的媒体对象
         * @param generator JSON 生成器
         * @param provider  序列化提供器，当前未使用
         * @throws IOException 写入 JSON 失败时抛出
         */
        @Override
        public void serialize(Media media, JsonGenerator generator, SerializerProvider provider) throws IOException {

            generator.writeStartObject();

            if (media.getId() != null) {
                generator.writeStringField(FIELD_ID, media.getId());
            }

            MimeType mimeType = media.getMimeType();
            generator.writeObjectFieldStart(FIELD_MIME_TYPE);
            generator.writeStringField(FIELD_TYPE, mimeType.getType());
            generator.writeStringField(FIELD_SUBTYPE, mimeType.getSubtype());
            generator.writeObjectField(FIELD_PARAMETERS, mimeType.getParameters());
            generator.writeEndObject();

            // data 为 byte[] 时输出整型数组（与反序列化对称），其余（URI 等）输出字符串
            Object data = media.getData();
            if (data instanceof byte[] bytes) {
                generator.writeArrayFieldStart(FIELD_DATA);
                for (byte item : bytes) {
                    generator.writeNumber(item);
                }
                generator.writeEndArray();
            } else {
                generator.writeStringField(FIELD_DATA, String.valueOf(data));
            }

            generator.writeStringField(FIELD_NAME, media.getName());

            generator.writeEndObject();

        }

    }

}
