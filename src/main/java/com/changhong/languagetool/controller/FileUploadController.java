package com.changhong.languagetool.controller;


import com.changhong.languagetool.bean.Logging;
import com.changhong.languagetool.mapper.LanguageMapper;
import com.changhong.languagetool.mapper.LoggingMapper;
import com.changhong.languagetool.service.LoggingService;
import com.changhong.languagetool.service.ReadExcel;
import com.changhong.languagetool.service.TransitionExcel;
import com.changhong.languagetool.service.UpdateFromExcelFile;
import com.changhong.languagetool.utils.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class FileUploadController {

    private  final
    TransitionExcel transitionExcel ;

    private final LanguageMapper languageMapper;

    private final LoggingMapper loggingMapper;
    private final ReadExcel readExcel ;
    private final LoggingService loggingService;
    private final
    UpdateFromExcelFile updateFromExcelFile;
    @Autowired
    public FileUploadController(ReadExcel readExcel, TransitionExcel transitionExcel, UpdateFromExcelFile updateFromExcelFile, LoggingMapper loggingMapper, LoggingService loggingService, LanguageMapper languageMapper) {
        this.readExcel = readExcel;
        this.transitionExcel = transitionExcel;
        this.updateFromExcelFile = updateFromExcelFile;
        this.loggingMapper = loggingMapper;
        this.loggingService = loggingService;
        this.languageMapper = languageMapper;
    }

    //更新数据库
    @PostMapping("/fileupdate")
    public List<String > fileUpdate(@RequestParam("fileUpdate") MultipartFile file ,
                             @RequestParam("type")String  type){

        System.out.println(type);
        List<String > res = new ArrayList<>();
        if(file.isEmpty()){
            res.add("更新操作 :"+type+"-->false");
            return  res;
        }
        try {
            //if()
            boolean b = ResultMessage.res.removeAll(ResultMessage.res);
            updateFromExcelFile.updateDBFromFile(file.getInputStream(),file.getOriginalFilename(),type);
            loggingService.addLogging("updateFile");
            if(type.equals("检查覆盖")&&ResultMessage.res.size()!=0){
                return ResultMessage.res;
            }
            res.add("更新操作 :"+type+"-->ok");

           return res;
        }catch (Exception e){
            e.printStackTrace();
            res.add("更新操作 :"+type+"-->false");
            return res;
        }


    }



    @PostMapping("/fileUpload")
    public String fileUpload(@RequestParam("fileName") MultipartFile file){

        List<String> list = languageMapper.selectCountryName();
        //删除数据库后只保留id
        if(list.size() > 1){
            return "false：数据库有文件存在，请删除数据库后在操作";
        }
        if(file.isEmpty()){
            return  "false：文件不能为空";
        }
        try {
            readExcel.readExcelFile(file);
            //Todo 日志追加
            loggingService.addLogging("allFile");
            List<String> allWords = languageMapper.getAllWords();

            String equalsWords = "";
            int k=0;
            //记录下重复的词条，然后再删除掉
            for(int i=0;i<allWords.size();i++){
                for(int j=i+1;j<allWords.size();j++){
                    if(!allWords.get(i).equals("?")&&allWords.get(i).equals(allWords.get(j))){
                        System.out.println(i+":"+ allWords.get(i)+"--->"+j+":"+allWords.get(j));
                        equalsWords += i+":"+ allWords.get(i)+"--->"+j+":"+allWords.get(j)+"<hr>";
                        languageMapper.deleteWords(allWords.get(i));
                    }
                }
            }
            if(!equalsWords.equals("")){
                return "false：存在相同的词条，请确认词条，确保词条库唯一。词条为<hr>"+equalsWords;
            }
            return  "上次词条库成功";
        }catch (Exception e){
          e.printStackTrace();
          return  "false：服务器出错";
        }

     //   String fileName = file.getOriginalFilename();
        //文件保存路径，到时候是保存到服务器上的
//        String path = "D:/fileUpload" ;
//        int size = (int)file.getSize();
//        System.out.println(fileName + "-->" + size);
//
//        File dest = new File(path + "/" + fileName);
//        //判断文件父目录是否存在
//        if(!dest.getParentFile().exists()){
//            boolean mkdir = dest.getParentFile().mkdir();
//            if(!mkdir) return  "false";
//        }
//        try {
//            file.transferTo(dest); //保存文件
//            return "true";
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            return "false";
//        }

   //     return  "true";

    }
    //转换
    @PostMapping("/updatefile")
    public String fileUpload(@RequestParam("fileName1") MultipartFile file,
                             @RequestParam("fileName2") String  userFileName,
                             HttpServletResponse response) throws  IOException{

        String  fileName = file.getOriginalFilename();
        System.out.println("fileName ---- >" + fileName);
        System.out.println("userFileName ---- >" + userFileName);
        //userFileName = userFileName.split(".")[0];
        String filename=userFileName + ".xlsx";
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" +   java.net.URLEncoder.encode(filename,"UTF-8"));
        OutputStream os = null; //输出流
        InputStream is = null; //输入流
        try {
                os = response.getOutputStream();
                is =  file.getInputStream();
                transitionExcel.transitionFile(is,os);
                return "true";

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("----------file download---" + filename);

        return "false";

    }



    @PostMapping("multifileUpload")
    /*public @ResponseBody String multifileUpload(@RequestParam("fileName")List<MultipartFile> files) */
    public  String multifileUpload(HttpServletRequest request){


        //文件保存路径，到时候是保存到服务器上的
        String path = "D:/fileUpload" ;
        List<MultipartFile> files = ((MultipartHttpServletRequest)request).getFiles("fileName");
        if(files.isEmpty()){
            return "false";
        }
        for(MultipartFile file:files){
            String fileName = file.getOriginalFilename();
            int size = (int) file.getSize();
            System.out.println(fileName + "-->" + size);

            if(file.isEmpty()){
                return "false";
            }else{
                File dest = new File(path + "/" + fileName);
                if(!dest.getParentFile().exists()){ //判断文件父目录是否存在
                    dest.getParentFile().mkdir();
                }
                try {
                    file.transferTo(dest);
                }catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return "false";
                }
            }
        }
        return "true";
    }

//    @GetMapping("/test")
//    public String tests() throws  IOException{
//
//        String fileName = "C:/Users/Administrator/Desktop/citiaoall.xlsx";
//        String equalsName="C:/Users/Administrator/Desktop/equals.xlsx";
//        String disequalsName = "C:/Users/Administrator/Desktop/disequals.xlsx";
//        InputStream is = new FileInputStream(new File(fileName));
//        OutputStream equalsOS = new FileOutputStream(new File(equalsName));
//        OutputStream disequalsOS = new FileOutputStream(new File(disequalsName));
//
//        try {
//            transitionExcel.test(is,equalsOS,disequalsOS);
//            return "true";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "false";
//        }
//
//
//    }
}
