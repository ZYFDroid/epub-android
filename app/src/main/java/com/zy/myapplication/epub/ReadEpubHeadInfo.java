package com.zy.myapplication.epub;

import android.content.Context;
import android.text.TextUtils;

import com.zy.myapplication.utils.FileUtils;
import com.zy.myapplication.utils.LogUtils;
import com.zy.myapplication.utils.XmlUtils;
import com.zy.myapplication.utils.ZipUtils;

import java.io.File;

/**
 * @作者: JJ
 * @创建时间: 2018/9/12 上午11:58
 * @Version 1.0
 * @描述: 直接获取Epub的书名+封面图片。
 */
public class ReadEpubHeadInfo {

    /**
     * 存储content.opf文件路径
     */
    public static final String META_INF_CONTAINER = "META-INF/container.xml";
    /**
     * 默认解压后进行暂存的地址
     */
    private static String SAVE_INFO_PATH = "/sdcard/cache/";

    public static void setCachePath(Context ctx){
        SAVE_INFO_PATH = ctx.getCacheDir().getAbsolutePath()+"/cache/";
        SAVE_IMAGE_PATH = ctx.getCacheDir().getAbsolutePath()+"/result/";
    }

    /**
     * 默认图片存放路径
     */
    private static String SAVE_IMAGE_PATH = "/sdcard/cache2/";

    public String getSaveInfoPath() {
        return SAVE_INFO_PATH;
    }

    public void setSaveInfoPath(String saveInfoPath) {
        SAVE_INFO_PATH = saveInfoPath;
        SAVE_IMAGE_PATH = new File(saveInfoPath,"cover").getAbsolutePath();
        new File(SAVE_IMAGE_PATH).mkdirs();
        new File(SAVE_INFO_PATH).mkdirs();
    }

    public ReadEpubHeadInfo() {

    }

    public static BookModel getePubBook(String ePubPath) {
        //路径是否存在
        if (TextUtils.isEmpty(ePubPath))
            return null;
        //是否是epub书籍
        if (!FileUtils.getFileExtension(ePubPath).equals("epub")) {
            return null;
        }
        BookModel book = new BookModel();
        //思路是将指定部分文件解压至指定目录，上一次如果解析过会有残留，当然也可以不清。
        if (FileUtils.isDir(SAVE_INFO_PATH)) {
            FileUtils.deleteDir(SAVE_INFO_PATH);
        }
        try {
            //存储content.opf文件路径信息
            String contentOpfPath = "";
            //1.解压MEAT-INF文件，解析container.xml的rootfile标签，获取content.opf的路径。
            if (ZipUtils.zipSpecifiedFile(ePubPath, SAVE_INFO_PATH, META_INF_CONTAINER)) {
                contentOpfPath = XmlUtils.xmlSubtagNameAnalysis(SAVE_INFO_PATH + META_INF_CONTAINER, "rootfiles", "rootfile", "full-path");
            } else {
                LogUtils.e("epub解析", ePubPath + "解析错误，请检查书本");
                return null;
            }
            //2.解压获取到的content.opf路径，并用xml解析获取书名、作者等信息
            if (ZipUtils.zipSpecifiedFile(ePubPath, SAVE_INFO_PATH, contentOpfPath)) {
                book.setName(XmlUtils.xmlSubtagNameAnalysis(SAVE_INFO_PATH + contentOpfPath, "metadata", "title", null));
                book.setAuthor(XmlUtils.xmlSubtagNameAnalysis(SAVE_INFO_PATH + contentOpfPath, "metadata", "creator", null));
                //3.获取封面图片路径
                String imgXmlFlag = XmlUtils.xmlSubtagConditionAnalysis(SAVE_INFO_PATH + contentOpfPath, "metadata", "name", "cover", "content");
                if (imgXmlFlag != null) {
                    String imgPath = "";
                    String[] content = contentOpfPath.split("/");
                    for (int i = 0; i < content.length - 1; i++) {
                        imgPath += content[i] + "/";
                    }

                    String[] sourceBookName = ePubPath.split("/");

                    imgPath += XmlUtils.xmlSubtagConditionAnalysis(SAVE_INFO_PATH + contentOpfPath, "manifest", "id", imgXmlFlag, "href");
                    String saveImagePath = SAVE_IMAGE_PATH + FileUtils.delFileSuffix(sourceBookName[sourceBookName.length - 1]) + "/";
                    //4.根据路径解压图片
                    if (ZipUtils.zipSpecifiedFile(ePubPath, saveImagePath, imgPath)) {
                        book.setCover(saveImagePath + imgPath);
                    } else {
                        book.setCover(null);
                    }
                }
            } else {
                LogUtils.e("epub解析", ePubPath + "解析错误，请检查书本（可能原因，书籍被加密）");
                return null;
            }

        } catch (Exception e) {
            return null;
        }
        if (book.getName() == null) {
            //获取文件名，作为备用，如果获取不到书名的话用文件名代替
            book.setName(new File(ePubPath).getName());
        }
        return book;
    }


}