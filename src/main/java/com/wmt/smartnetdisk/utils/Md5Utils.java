package com.wmt.smartnetdisk.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类
 *
 * @author wmt
 * @since 1.0.0
 */
@Slf4j
public class Md5Utils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private Md5Utils() {
        throw new IllegalStateException("工具类不允许实例化");
    }

    /**
     * 计算文件 MD5
     *
     * @param file MultipartFile 文件
     * @return MD5 字符串（32位小写）
     */
    public static String calculateMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return calculateMd5(inputStream);
        } catch (IOException e) {
            log.error("计算文件MD5失败", e);
            return null;
        }
    }

    /**
     * 计算输入流 MD5
     *
     * @param inputStream 输入流
     * @return MD5 字符串（32位小写）
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }

    /**
     * 计算字符串 MD5
     *
     * @param content 字符串内容
     * @return MD5 字符串（32位小写）
     */
    public static String calculateMd5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes());
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
