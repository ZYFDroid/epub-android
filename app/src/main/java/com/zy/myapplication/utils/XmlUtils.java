package com.zy.myapplication.utils;

import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @作者: JJ
 * @创建时间: 2018/11/10 2:47 PM
 * @Version 1.0
 * @描述: XML解析工具类
 */
public class XmlUtils {

    /**
     * 找到指定标签的属性值
     * **************例***************
     * xml内容为:
     * <metadata xmlns:dc="xxx">
     * <dc:identifier id="Bookid" bookname="test">Hello World!</dc:identifier>
     * </metadata>
     * 使用:
     * dom4jReadXMLFile("路径","metadata","identifier","id")：return = "Bookid"
     * dom4jReadXMLFile("路径","metadata","identifier",null)：retrun = "Hello World!"
     * *******************************
     *
     * @param xmlPath              xml文件路径
     * @param fatherNode           父标签名
     * @param subtagName           子标签名
     * @param subtagAttributesName 属性名  为null时，获取的是该子标签的内容
     * @return 属性值 | 标签内容
     * @throws
     */
    public static String xmlSubtagNameAnalysis(final String xmlPath, final String fatherNode, final String subtagName, final String subtagAttributesName) throws DocumentException {
        return xmlAnalysis(xmlPath, fatherNode, subtagName, subtagAttributesName, null, null);
    }

    /**
     * 根据条件 获取子标签已知属性名的属性值
     * 条件：子标签属性名与之对应的属性值
     * **************例***************
     * xml内容为:
     * <metadata xmlns:dc="xxx">
     * <dc:identifier id="Bookid" bookname="test">Hello World!</dc:identifier>
     * </metadata>
     * 使用:
     * dom4jReadXMLFile("路径","metadata","id","Bookid","bookname")：return = "test"
     * *******************************
     *
     * @param xmlPath               xml文件路径
     * @param fatherNode            父标签名
     * @param subtagAttributesName  子标签属性名
     * @param subtagAttributesValue 子标签属性值
     * @param conditionName         要寻找的子标签的属性值所对应的属性名
     * @return 属性名
     * @throws DocumentException
     */
    public static String xmlSubtagConditionAnalysis(final String xmlPath, final String fatherNode, final String subtagAttributesName, final String subtagAttributesValue, final String conditionName) throws DocumentException {
        return xmlAnalysis(xmlPath, fatherNode, null, subtagAttributesName, subtagAttributesValue, conditionName);
    }

    private static String xmlAnalysis(final String xmlPath, final String fatherNode, final String subtagName, final String subtagAttributesName, final String subtagAttributesValue, final String conditionName) throws DocumentException {
        boolean isSubTagNameEmpty = TextUtils.isEmpty(subtagName);

        if (TextUtils.isEmpty(xmlPath))
            return null;
        if (TextUtils.isEmpty(fatherNode))
            return null;
        if (TextUtils.isEmpty(subtagAttributesName) && isSubTagNameEmpty)
            return null;

        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(xmlPath));
        // 通过document对象获取根节点bookstore
        Element node = document.getRootElement();
        Iterator<Element> it = node.element(fatherNode).elementIterator();
        // 获取element的id属性节点对象
        if (it != null) {
            while (it.hasNext()) {
                Element e = it.next();
                List<Attribute> list = e.attributes();
                //子标签名不为空时
                if (!isSubTagNameEmpty) {
                    //比对子标签名
                    if (subtagName.equals(e.getName())) {
                        //若是子标签的属性名不为空
                        if (subtagAttributesName != null) {
                            for (Attribute attr : list) {
                                //                            System.out.println(attr.getName() + "=" + attr.getValue() + "\n");
                                //查询属性名
                                if (attr.getName().equals(subtagAttributesName)) {
                                    return attr.getValue();
                                }
                            }
                            return null;
                        }
                        //若是子标签的属性名为空，就是获取其子标签的内容
                        else {
                            return e.getStringValue();
                        }
                    }
                }
                //子标签为空，就根据子标签的属性名进行查找
                else {
                    for (Attribute attr : list) {
                        //                    System.out.println(attr.getName() + "=" + attr.getValue() + "\n");
                        if (attr.getName().equals(subtagAttributesName) && attr.getValue().equals(subtagAttributesValue)) {
                            for (Attribute attrs : list) {
                                if (attrs.getName().equals(conditionName)) {
                                    return attrs.getValue();
                                }
                            }
                        }
                    }
                }

            }

        }
        return null;
    }

    /**
     * 遍历当前节点元素下面的所有(元素的)子节点
     *
     * @param node
     */
    private void listNodes(final Element node) {
        System.out.println("当前节点的名称：：" + node.getName());
        // 获取当前节点的所有属性节点
        List<Attribute> list = node.attributes();
        // 遍历属性节点
        for (Attribute attr : list) {
            System.out.println(attr.getName() + "=" + attr.getValue() + "\n");
        }

        if (!(node.getTextTrim().equals(""))) {
            System.out.println("文本内容：：：：" + node.getText());
        }

        // 当前节点下面子节点迭代器
        Iterator<Element> it = node.elementIterator();
        // 遍历
        while (it.hasNext()) {
            // 获取某个子节点对象
            Element e = it.next();
            // 对子节点进行遍历
            listNodes(e);
        }
    }

}