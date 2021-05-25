package com.hyperq.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;

public class FileUtil {

    public static void download(final String filename, final HttpServletResponse res) throws IOException {

        final OutputStream outputStream = res.getOutputStream();
        final byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        try {
            bis  = new BufferedInputStream(new FileInputStream(new File(filename)));
            int i = bis.read(buff);
            while (i != -1) {
                outputStream.write(buff, 0, i);
                outputStream.flush();
                i = bis.read(buff);
            }
        }finally{
            bis.close();
        }

    }


    public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
        InputStream is = new FileInputStream(filePath);
        String line; // 用来保存每行读取的内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine(); // 读取第一行
        while (line != null) { // 如果 line 为空说明读完了
            buffer.append(line); // 将读到的内容添加到 buffer 中
            buffer.append("\n"); // 添加换行符
            line = reader.readLine(); // 读取下一行
        }
        reader.close();
        is.close();
    }

    /**
     * 读取文本文件内容
     * @param filePath 文件所在路径
     * @return 文本内容
     * @throws IOException 异常
     * @author cn.outofmemory
     * @date 2013-1-7
     */
    public static String readFile(String filePath) throws IOException {
        StringBuffer sb = new StringBuffer();
        FileUtil.readToBuffer(sb, filePath);
        return sb.toString();
    }



}