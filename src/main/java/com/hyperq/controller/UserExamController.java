package com.hyperq.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.bytebuddy.description.ByteCodeElement.Token.TokenList;
import net.lingala.zip4j.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ThreadUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import com.hyperq.utilities.FileUtil;
import com.hyperq.entity.ReturnData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static java.util.stream.Collectors.toList;


@CrossOrigin
@Controller
@Api(value = "检测接口", tags = "UserExamController", description = "检测接口相关")
@RequestMapping(value = "/web/api/ExamService")
public class UserExamController {
    public static final int FULL_SLEEP_RANGE_LENGTH = 3;
    public static final float MORNING_TIME_CLOCK = 3.0f;
    public static final int HOURS_OF_DAY = 24;

    private static final Logger logger = LoggerFactory.getLogger(UserExamController.class);

    @Value("${hyperq.DataDirectory}")
    private String dataDirectory;

    @Value("${hyperq.zipUrl}")
    private String zipUrl;

    @ApiOperation(value = "上传文件。", notes = "上传文件")
    @RequestMapping(value = "/uploadFile/{examUuid}/{fileType}", method = RequestMethod.POST)
    @ResponseBody
    synchronized public ReturnData<String> uploadFile(
            @ApiParam(required = true, name = "examUuid", value = "examUuid") @PathVariable(value = "examUuid") String examUuid,
            @ApiParam(name = "fileType", value = "fileType") @PathVariable(value = "fileType") String fileType,
            MultipartFile blFile) {

        fileType = StringUtils.isAllBlank(fileType) ? getFileExtension(blFile) : fileType;
        if (StringUtils.isAllBlank(fileType)) {
            fileType = "bsp";
        }

        ReturnData<String> returnData = new ReturnData<String>();
        returnData.setSuccess(true);

        String zipDirectory = dataDirectory + "/" + fileType;
        String xmlDirectory = dataDirectory + "/" + "xml";
        makeSureFoldersExists(zipDirectory);
        makeSureFoldersExists(xmlDirectory);

        String zipFileName = new File(zipDirectory).getAbsolutePath() + "/" + getTargetFileName(examUuid, fileType);
        String xmlFileName = new File(xmlDirectory).getAbsolutePath() + "/";// + getTargetFileName(examUuid,"xml")

        try {
            File zipFile = new File(zipFileName);
            zipFile.deleteOnExit();
            zipFile.createNewFile();
            blFile.transferTo(zipFile);

            ZipFile zipFileToExtra = new ZipFile(zipFileName);
            zipFileToExtra.setCharset(Charset.forName("GBK"));
            File xmlFile = new File(xmlFileName);
            xmlFile.deleteOnExit();

            zipFileToExtra.extractAll(xmlFileName);

            List<File> sortedFiles = getFileSort(xmlFileName);
            String pathOfFileToAnalyse = sortedFiles.get(0).getAbsolutePath();

            String cmdLine = "D: & cd D:/Analyzer/AnalyzerStress/bin/ & HyperQAnalysis";
            cmdLine += " -edan " + pathOfFileToAnalyse;

            checkProcess("HyperQAnalysis.exe");

            Process p = Runtime.getRuntime().exec("cmd /c " + cmdLine);
            // Thread t = new Thread(new Runnable() {
            //     public void run() {
            //         File bspFile;
            //         boolean bspFileExists;
            //         do {
            //             bspFile = new File(pathOfFileToAnalyse.replace(".xml","_bsp"));
            //             bspFileExists = bspFile.exists();
            //             try{
            //                 Thread.sleep(10);
            //             } catch (Exception e) {
            //                 e.printStackTrace();
            //             }
            //         } while (!bspFileExists);
            //     }
            // });
            // t.start();

            //p.waitFor();

            returnData.setDescription("文件提交成功。");
            returnData.setData(Arrays.asList(zipFileName));
        } catch (Exception e) {
            e.printStackTrace();
            returnData.setDescription("文件提交失败，内部错误。");
            returnData.setSuccess(false);
        }
        return returnData;
    }

    @RequestMapping(value = "/download/{examUuid}/{fileType}", method = RequestMethod.GET)
    public void download(
            @ApiParam(required = true, name = "examUuid", value = "examUuid") @PathVariable(value = "examUuid") String examUuid,
            @ApiParam(required = true, name = "fileType", value = "文件类型：xml,bsp,pdf.zip.") @PathVariable(value = "fileType") String fileType,
            final HttpServletResponse response) throws IOException {
        String type = fileType;
        String directory = dataDirectory + "/" + fileType;
        File file = new File(new File(directory).getAbsolutePath() + "/" + getTargetFileName(examUuid, type));
        if (!file.exists() && type.equalsIgnoreCase("xml")) {
            type = "zip";
            directory = dataDirectory + "/" + type;
        }

        response.reset();
        response.setHeader("Content-type", type);

        response.setContentType("application/octet-stream;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + getTargetFileName(examUuid, type)
                + ";filename*=utf-8''" + URLEncoder.encode(getTargetFileName(examUuid, type), "utf-8"));

        file = new File(new File(directory).getAbsolutePath() + "/" + getTargetFileName(examUuid, type));
        FileUtil.download(file.getAbsolutePath(), response);
    }

    private void makeSureFoldersExists(String directory) {

        File folderType = new File(directory);
        if (!folderType.exists()) {
            folderType.mkdir();
        }
    }

    private String getTargetFileName(String examUuid, String type) {
        return examUuid + "." + type;
    }

    private String getFileExtension(MultipartFile cFile) {
        String originalFileName = cFile.getOriginalFilename();
        return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private List<File> getFileSort(String path) {

        List<File> list = getFiles(path, new ArrayList<File>());
        list.stream().filter(p->p.getName().contains(".xml")).collect(toList());

        if (list != null && list.size() > 0) {
            Collections.sort(list, new Comparator<File>() {
                public int compare(File file, File newFile) {
                    if (file.lastModified() < newFile.lastModified()) {
                        return 1;
                    } else if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });

        }

        return list;
    }

    private List<File> getFiles(String realpath, List<File> files) {

        File realFile = new File(realpath);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles();
            for (File file : subfiles) {
                if (file.isDirectory()) {
                    getFiles(file.getAbsolutePath(), files);
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * 检查进程是否存在，存在则杀死进程
     * 
     * @param procName
     */
    public String checkProcess(String procName) {
        String result = "";
        // 判断是否存在进程
        Boolean existProc = false;
        BufferedReader bufferedReader = null;
        try {
            Process proc = Runtime.getRuntime().exec("tasklist -fi " + '"' + "imagename eq " + procName + '"');
            bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(procName)) {
                    existProc = true;// 存在
                }
            }
        } catch (Exception ex) {
            result = "查询程序进程异常：" + ex.getMessage();
            logger.error("查询程序进程异常：" + ex.getMessage());
            return result;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                }
            }
        }

        // 存在，则先杀死该进程
        if (existProc) {
            BufferedReader br = null;
            try {
                if (StringUtils.isNotBlank(procName)) {
                    // 执行cmd命令
                    String command = "taskkill /F /IM " + procName;
                    Runtime runtime = Runtime.getRuntime();
                    Process process = runtime.exec("cmd /c " + command);
                    br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                    String line = null;
                    StringBuilder build = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        build.append(line);
                    }
                }
            } catch (Exception e) {
                result = "关闭程序进程异常：" + e.getMessage();
                logger.error("关闭程序进程异常：" + e.getMessage());
                return result;
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return result;
    }
}
