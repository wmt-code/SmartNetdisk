package com.wmt.smartnetdisk.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 带 MD5 计算功能的输入流包装类
 * 在读取数据的同时计算 MD5，避免重复读取文件
 *
 * @author wmt
 * @since 1.0.0
 */
public class DigestInputStream extends FilterInputStream {

    private final MessageDigest digest;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public DigestInputStream(InputStream in) throws NoSuchAlgorithmException {
        super(in);
        this.digest = MessageDigest.getInstance("MD5");
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            digest.update((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = in.read(b, off, len);
        if (result != -1) {
            digest.update(b, off, result);
        }
        return result;
    }

    /**
     * 获取已读取数据的 MD5 值
     * 注意：此方法应在流读取完毕后调用
     *
     * @return MD5 字符串（32位小写）
     */
    public String getMd5() {
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
