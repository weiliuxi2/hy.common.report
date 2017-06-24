package org.hy.common.report.bean;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hy.common.Help;
import org.hy.common.MethodReflect;
import org.hy.common.PartitionMap;
import org.hy.common.report.ExcelHelp;
import org.hy.common.report.event.ValueListener;





/**
 * 报表模板信息 
 *
 * @author      ZhengWei(HY)
 * @createDate  2017-03-15
 * @version     v1.0
 *              v2.0  2017-06-21  优化：通过isSafe参数控制，放弃一些非必要的效验来提高性能
 *                                优化：启用对SXSSFWorkbook工作薄的支持大数据量
 */
public class RTemplate implements Comparable<RTemplate>
{
    
    /** 系统固定变量名称：数据行号的变量名称。下标从1开始 */
    public final static String         $ValueName_RowNo              = "RowNo__";
    
    /** 系统固定变量名称：数据索引号的变量名称。下标从0开始 */
    public final static String         $ValueName_RowIndex           = "RowIndex__";
    
    /** 系统固定变量名称：数据总量的变量名称 */
    public final static String         $ValueName_RowCount           = "RowCount__";
    
    /** 系统固定变量名称：数据小计总量的变量名称 */
    public final static String         $ValueName_RowSubtotalCount   = "RowSubtotalCount__";
    
    
    /** 纵深扩展--表示从左到右的方向，一行或多行为一个对象数据 */
    public final static Integer       $Direction_Vertical            = 0;
    
    /** 横向扩展--表示从上到下的方向，一列或多列为一个对象数据 */
    public final static Integer       $Direction_Horizontal          = 1;
    
    
    
    /** 模板名称 */
    private String                     name;
    
    /** Excel文件版本(1.xls  2.xlsx) */
    private String                     excelVersion;
    
    /** 模板文件的名称(全路径+文件名称) */
    private String                     excelFileName;
    
    /** 报表模板对应的工作表索引位置（下标从零开始） */
    private Integer                    sheetIndex;
    
    /** 
     * Excel数据的方向。
     * 0：纵深扩展--表示从左到右的方向，一行或多行为一个对象数据。此为默认值
     * 1：横向扩展--表示从上到下的方向，一列或多列为一个对象数据
     */
    private Integer                    direction;
    
    /** 报表标题的开始行号（包括此行）。下标从零开始 */
    private Integer                    titleBeginRow;
    
    /** 报表标题的结束行号（包括此行）。下标从零开始 */
    private Integer                    titleEndRow;
    
    /** 报表数据的开始行号（包括此行）。下标从零开始 */
    private Integer                    dataBeginRow;
    
    /** 报表数据的结束行号（包括此行）。下标从零开始 */
    private Integer                    dataEndRow;
    
    /** 报表数据的开始列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况 */
    private Integer                    dataBeginCol;
    
    /** 报表数据的结束列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况 */
    private Integer                    dataEndCol;
    
    /** 报表小计的开始行号（包括此行）。下标从零开始 */
    private Integer                    subtotalBeginRow;
    
    /** 报表小计的结束行号（包括此行）。下标从零开始 */
    private Integer                    subtotalEndRow;
    
    /** 合计内容的开始行号（包括此行）。下标从零开始 */
    private Integer                    totalBeginRow;
    
    /** 合计内容的结束行号（包括此行）。下标从零开始 */
    private Integer                    totalEndRow;
    
    /** 报表数据的Java类型 */
    private String                     dataClass;
    
    /** 值的标记。默认为一个冒号：":" */
    private String                     valueSign;
    
    /** 是要安全？还是要性能（默认为：性能） */
    private Boolean                    isSafe;
    
    /** 
     * 当为大数据量导出时，建议使用 SXSSFWorkbook
     * POI对excel的导出操作，一般只使用HSSFWorkbook以及SXSSFWorkbook，
     * HSSFWorkbook用来处理较少的数据量，
     * SXSSFWorkbook用来处理大数据量以及超大数据量的导出
     * 
     * 默认为：true
     */
    private Boolean                    isBig;

    /** 
     * 当使用 SXSSFWorkbook 时（this.isBig=true），创建对象new SXSSFWorkbook(rowAccessWindowSize)入参。
     * 保持在内存中的行数，直到刷新为止
     */
    private Integer                    rowAccessWindowSize;
    
    
    
    /** 
     * 变量自定义处理事件
     * Map.key  为变量名称 
     */
    private Map<String ,ValueListener> valueListeners;
    
    
    
    /** 报表模板信息对应的工作表对象(一般只初始加载一次) */
    private Sheet                      templateSheet; 
    
    /** 解释的值的反射方法集合(一般只初始加载一次) */
    private Map<String ,RCell>         valueMethods;
    
    /** 按 this.valueSign 生成的系统变量名称 */
    private Map<String ,String>        valueNames;
    
    
    
    public RTemplate()
    {
        this.sheetIndex          = 0;
        this.direction           = 0;
        this.templateSheet       = null;
        this.excelVersion        = null;
        this.valueMethods        = new LinkedHashMap<String ,RCell>();
        this.valueNames          = new Hashtable<String ,String>();
        this.valueListeners      = new Hashtable<String ,ValueListener>();
        this.isSafe              = false;
        this.isBig               = true;
        this.rowAccessWindowSize = SXSSFWorkbook.DEFAULT_WINDOW_SIZE;
        this.setValueSign(":");
    }
    
    
    
    /**
     * 创建报表数据的Java对象实例
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-05-08
     * @version     v1.0
     *
     * @return
     */
    public Object newObject()
    {
        try
        {
            return Class.forName(this.dataClass).newInstance();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return null;
    }
    
    
    
    /**
     * 获取报表模板对应的工作表
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-16
     * @version     v1.0
     *
     * @return
     */
    public synchronized Sheet getTemplateSheet()
    {
        if ( null == this.templateSheet )
        {
            List<Sheet> v_Sheets = ExcelHelp.read(this.excelFileName);
            
            if ( Help.isNull(v_Sheets) )
            {
                this.templateSheet = null;
            }
            else
            {
                this.templateSheet = v_Sheets.get(this.sheetIndex);
            }
            
            this.init();
            
            this.getExcelVersion();
        }
        
        return this.templateSheet;
    }
    
    
    
    /**
     * 初始化
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     */
    private void init()
    {
        try
        {
            PartitionMap<String ,RCell> v_ExcelDatas = ExcelHelp.readDatas(this.getTemplateSheet());
            List<String>                v_TempDatas  = Help.toListKeys(v_ExcelDatas);
            Class<?>                    v_JavaClass  = Help.forName(this.dataClass);
            
            for (int i=v_TempDatas.size()-1; i>=0; i--)
            {
                final String v_Value = v_TempDatas.get(i);
                
                if ( this.valueNames.containsKey(v_Value) )
                {
                    // 系统变量名称不解释
                    continue;
                }
                
                if ( this.valueMethods.containsKey(v_Value) )
                {
                    // 防止重复解释
                    continue;
                }
                
                if ( v_Value.length() >= this.valueSign.length() + 1 && v_Value.startsWith(this.valueSign) )
                {
                    RCell     v_RCell     = new RCell();
                    String    v_ValueName = v_Value.substring(this.valueSign.length());
                    String [] v_Fors      = v_ValueName.split("\\[\\]");
                    boolean   v_IsPull    = false;
                    
                    if ( v_Fors.length >= 2 )
                    {
                        MethodReflect v_ForMR         = new MethodReflect(v_JavaClass ,v_Fors[0] ,true ,MethodReflect.$NormType_Getter);
                        String        v_Iterator      = "";
                        String        v_IteratorSize  = "";
                        int           v_GenericsIndex = 0;
                        
                        if (      MethodReflect.isExtendImplement(v_ForMR.getReturnType() ,List.class) )
                        {
                            v_Iterator      = v_Fors[0] + ".$iterator";
                            v_IteratorSize  = v_Fors[0] + ".$size";
                            v_ValueName     = v_Fors[1].substring(1);
                            v_GenericsIndex = 0;
                        }
                        else if ( MethodReflect.isExtendImplement(v_ForMR.getReturnType() ,Set.class) )
                        {
                            v_Iterator      = v_Fors[0] + ".$iterator";
                            v_IteratorSize  = v_Fors[0] + ".$size";
                            v_ValueName     = v_Fors[1].substring(1);
                            v_GenericsIndex = 0;
                        }
                        else if ( MethodReflect.isExtendImplement(v_ForMR.getReturnType() ,Map.class) )
                        {
                            v_Iterator      = v_Fors[0] + ".$values.$iterator";
                            v_IteratorSize  = v_Fors[0] + ".$size";
                            v_ValueName     = v_Fors[1].substring(1);
                            v_GenericsIndex = 1;
                        }
                        
                        v_RCell.setIteratorSizeMethod(new MethodReflect(v_JavaClass ,v_IteratorSize ,true ,MethodReflect.$NormType_Getter));
                        v_RCell.setIteratorMethod(    new MethodReflect(v_JavaClass ,v_Iterator     ,true ,MethodReflect.$NormType_Getter));
                        
                        Class<?> v_ForElementJavaClass = MethodReflect.getGenericsReturn(v_ForMR.getReturnMethod() ,v_GenericsIndex).getGenericType();
                        
                        try
                        {
                            v_RCell.setValueMethod(new MethodReflect(v_ForElementJavaClass ,v_ValueName ,true ,MethodReflect.$NormType_Getter));
                            v_IsPull = true;
                        }
                        catch (Exception exce)
                        {
                            // 有可能没有Getter方法
                        }
                        
                        try
                        {
                            v_RCell.setValueSetMethod(new MethodReflect(v_ForElementJavaClass ,v_ValueName ,true ,MethodReflect.$NormType_Setter));
                            v_IsPull = true;
                        }
                        catch (Exception exce)
                        {
                            // 有可能没有Setter方法
                        }
                    }
                    else
                    {
                        try
                        {
                            v_RCell.setValueMethod(new MethodReflect(v_JavaClass ,v_ValueName ,true ,MethodReflect.$NormType_Getter));
                            v_IsPull = true;
                        }
                        catch (Exception exce)
                        {
                            // 有可能没有Getter方法
                        }
                        
                        try
                        {
                            v_RCell.setValueSetMethod(new MethodReflect(v_JavaClass ,v_ValueName ,true ,MethodReflect.$NormType_Setter));
                            v_IsPull = true;
                        }
                        catch (Exception exce)
                        {
                            // 有可能没有Setter方法
                        }
                    }
                    
                    if ( v_IsPull )
                    {
                        this.valueMethods.put(v_Value ,v_RCell);
                    }
                    else
                    {
                        System.err.println("变量名称或占位符[" + v_Value + "]未匹配到对应数据结构中的属性值。");
                    }
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    /**
     * 判定变量名称是否存在
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_ValueName  变量名称
     * @return
     */
    public boolean isExists(String i_ValueName)
    {
        return i_ValueName.startsWith(this.valueSign);
        //return this.valueMethods.containsKey(i_ValueName) || this.valueNames.containsKey(i_ValueName);
    }
    
    
    
    /**
     * 通过变量名称反射出对应的数值
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_ValueName     变量名称
     * @param i_Datas         数据
     * @param i_RSystemValue  系统变量信息
     * @param io_RValue       小计循环迭代器
     * @return             
     */
    public RValue getValue(String i_ValueName ,Object i_Datas ,RSystemValue i_RSystemValue ,RValue io_RValue)
    {
        RCell  v_RCell  = this.valueMethods.get(i_ValueName);
        RValue v_RValue = io_RValue != null ? io_RValue : new RValue();
        
        if ( null != v_RCell && null != v_RCell.getValueMethod() )
        {
            try
            {
                if ( v_RCell.isFor() )
                {
                    if ( v_RValue.getIterator() == null )
                    {
                        v_RValue.setIterator((Iterator<?>)v_RCell.getIteratorMethod()    .invokeForInstance(i_Datas));
                        v_RValue.setIteratorSize(    (int)v_RCell.getIteratorSizeMethod().invokeForInstance(i_Datas));
                    }
                    
                    if ( v_RValue.getIterator().hasNext() )
                    {
                        Object v_ForElement = v_RValue.getIterator().next();
                        v_RValue.setValue(v_RCell.getValueMethod().invokeForInstance(v_ForElement));
                        v_RValue.setIteratorIndex(v_RValue.getIteratorIndex() + 1);
                    }
                }
                else
                {
                    v_RValue.setValue(v_RCell.getValueMethod().invokeForInstance(i_Datas));
                }
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else
        {
            String v_ValueName = this.valueNames.get(i_ValueName);
            
            if ( $ValueName_RowNo.equalsIgnoreCase(v_ValueName) )
            {
                v_RValue.setValue(String.valueOf(i_RSystemValue.getRowNo()));
            }
            else if ( $ValueName_RowIndex.equalsIgnoreCase(v_ValueName) )
            {
                v_RValue.setValue(String.valueOf(i_RSystemValue.getRowIndex()));
            }
            else if ( $ValueName_RowCount.equalsIgnoreCase(v_ValueName) )
            {
                v_RValue.setValue(String.valueOf(i_RSystemValue.getRowCount()));
            }
            else if ( $ValueName_RowSubtotalCount.equalsIgnoreCase(v_ValueName) )
            {
                v_RValue.setValue(String.valueOf(i_RSystemValue.getRowSubtotalCount()));
            }
        }
        
        return v_RValue;
    }
    
    
    
    /**
     * Excel转Java时，将Excel单位格中的数据映射并赋值给Java对象的属性
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-05-09
     * @version     v1.0
     *
     * @param i_ValueName  占位符名称
     * @param i_Value      Excel单位格中的数据
     * @param io_RowObj    Java对象
     * @return             映射赋值成功时返回true
     */
    public boolean setValue(String i_ValueName ,Object i_Value ,Object io_RowObj)
    {
        RCell v_RCell = this.valueMethods.get(i_ValueName);
        
        if ( null != v_RCell && null != v_RCell.getValueSetMethod() )
        {
            try
            {
                v_RCell.getValueSetMethod().invokeSetForInstance(io_RowObj ,i_Value);
                
                return true;
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return false;
    }
    
    
    
    /**
     * 获取标题的总行数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @return
     */
    public int getRowCountTitle()
    {
        return this.getRowCount(this.titleBeginRow ,this.titleEndRow);
    }
    
    
    
    /**
     * 获取数据的总行数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @return
     */
    public int getRowCountData()
    {
        return this.getRowCount(this.dataBeginRow ,this.dataEndRow);
    }
    
    
    
    /**
     * 获取数据的总列数。用于Excel转为Java对象，并且this.direction=1的情况
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-05-08
     * @version     v1.0
     *
     * @return
     */
    public int getColCountData()
    {
        return this.getRowCount(this.dataBeginCol ,this.dataEndCol);
    }
    
    
    
    /**
     * 获取数据的总行数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-27
     * @version     v1.0
     *
     * @return
     */
    public int getRowCountSubtotal()
    {
        return this.getRowCount(this.subtotalBeginRow ,this.subtotalEndRow);
    }
    
    
    
    /**
     * 获取合计的总行数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @return
     */
    public int getRowCountTotal()
    {
        return this.getRowCount(this.totalBeginRow ,this.totalEndRow);
    }
    
    
    
    /**
     * 获取的总行数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @param i_BeginRow  开始行号。下标从零开始
     * @param i_EndRow    结束行号。下标从零开始
     * @return            0表示不存在
     */
    public int getRowCount(Integer i_BeginRow ,Integer i_EndRow)
    {
        if ( null == i_BeginRow 
          || null == i_EndRow )
        {
            return 0;
        }
        
        if ( i_BeginRow.intValue() == i_EndRow.intValue() )
        {
            return 1;
        }
        
        return i_EndRow.intValue() - i_BeginRow.intValue() + 1;
    }
    
    
    
    /**
     * 添加自定义变量处理事件的监听者
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_Listener
     */
    public void addListener(ValueListener i_Listener)
    {
        if ( i_Listener == null )
        {
            throw new NullPointerException("ValueListener is null.");
        }
        
        if ( Help.isNull(i_Listener.getValueName()) )
        {
            throw new NullPointerException("ValueListener.getValueName() is null.");
        }
        
        this.valueListeners.put(i_Listener.getValueName() ,i_Listener);
    }
    
    
    
    /**
     * 获取自定义变量处理事件的监听者
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_ValueName  变量名称
     * @return
     */
    public ValueListener getListener(String i_ValueName)
    {
        return this.valueListeners.get(i_ValueName);
    }
    
    
    
    /**
     * 获取：模板名称
     */
    public String getName()
    {
        return name;
    }

    
    /**
     * 设置：模板名称
     * 
     * @param name 
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    
    /**
     * 获取：Excel文件版本(1.xls  2.xlsx)
     */
    public synchronized String getExcelVersion()
    {
        if ( Help.isNull(this.excelVersion) )
        {
            this.excelVersion = this.excelFileName.substring(this.excelFileName.lastIndexOf(".") + 1);
        }
        
        return excelVersion;
    }

    
    /**
     * 设置：Excel文件版本(1.xls  2.xlsx)
     * 
     * @param excelVersion 
     */
    public synchronized void setExcelVersion(String excelVersion)
    {
        this.excelVersion = excelVersion;
    }


    /**
     * 获取：模板文件的名称(全路径+文件名称)
     */
    public String getExcelFileName()
    {
        return excelFileName;
    }

    
    /**
     * 设置：模板文件的名称(全路径+文件名称)
     * 
     * @param excelFileName 
     */
    public void setExcelFileName(String excelFileName)
    {
        this.excelFileName = excelFileName;
    }

    
    /**
     * 获取：报表模板对应的工作表索引位置（下标从零开始）
     */
    public Integer getSheetIndex()
    {
        return sheetIndex;
    }

    
    /**
     * 设置：报表模板对应的工作表索引位置（下标从零开始）
     * 
     * @param sheetIndex 
     */
    public void setSheetIndex(Integer sheetIndex)
    {
        this.sheetIndex = sheetIndex;
    }
    
    
    /**
     * 获取：Excel数据的方向。
     * 0：表示从左到右的方向，一行或多行为一个对象数据。此为默认值
     * 1：表示从上到下的方向，一列或多列为一个对象数据
     */
    public Integer getDirection()
    {
        return direction;
    }
    
    
    /**
     * 设置：Excel数据的方向。
     * 0：表示从左到右的方向，一行或多行为一个对象数据。此为默认值
     * 1：表示从上到下的方向，一列或多列为一个对象数据
     * 
     * @param direction 
     */
    public void setDirection(Integer direction)
    {
        this.direction = direction;
    }
    
    
    /**
     * 获取：报表标题的开始行号（包括此行）。下标从零开始
     */
    public Integer getTitleBeginRow()
    {
        return titleBeginRow;
    }

    
    /**
     * 设置：报表标题的开始行号（包括此行）。下标从零开始
     * 
     * @param i_TitleBeginRow 
     */
    public void setTitleBeginRow(Integer i_TitleBeginRow)
    {
        this.titleBeginRow = i_TitleBeginRow;
        this.titleEndRow   = i_TitleBeginRow;
    }

    
    /**
     * 获取：报表标题的结束行号（包括此行）。下标从零开始
     */
    public Integer getTitleEndRow()
    {
        return titleEndRow;
    }

    
    /**
     * 设置：报表标题的结束行号（包括此行）。下标从零开始
     * 
     * @param titleEndRow 
     */
    public void setTitleEndRow(Integer titleEndRow)
    {
        this.titleEndRow = titleEndRow;
    }


    /**
     * 获取：报表数据的开始行号（包括此行）。下标从零开始
     */
    public Integer getDataBeginRow()
    {
        return dataBeginRow;
    }

    
    /**
     * 设置：报表数据的开始行号（包括此行）。下标从零开始
     * 
     * @param i_DataBeginRow 
     */
    public void setDataBeginRow(Integer i_DataBeginRow)
    {
        this.dataBeginRow = i_DataBeginRow;
        this.dataEndRow   = i_DataBeginRow;
    }

    
    /**
     * 获取：报表数据的结束行号（包括此行）。下标从零开始
     */
    public Integer getDataEndRow()
    {
        return dataEndRow;
    }

    
    /**
     * 设置：报表数据的结束行号（包括此行）。下标从零开始
     * 
     * @param dataEndRow 
     */
    public void setDataEndRow(Integer dataEndRow)
    {
        this.dataEndRow = dataEndRow;
    }
    
    
    /**
     * 获取：报表数据的开始列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况
     */
    public Integer getDataBeginCol()
    {
        return dataBeginCol;
    }


    /**
     * 设置：报表数据的开始列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况
     * 
     * @param dataBeginCol 
     */
    public void setDataBeginCol(Integer dataBeginCol)
    {
        this.dataBeginCol = dataBeginCol;
        this.dataEndCol   = dataBeginCol;
    }

    
    /**
     * 获取：报表数据的结束列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况
     */
    public Integer getDataEndCol()
    {
        return dataEndCol;
    }

    
    /**
     * 设置：报表数据的结束列号（包括此列）。下标从零开始。用于Excel转为Java对象，并且this.direction=1的情况
     * 
     * @param dataEndCol 
     */
    public void setDataEndCol(Integer dataEndCol)
    {
        this.dataEndCol = dataEndCol;
    }


    /**
     * 获取：报表小计的开始行号（包括此行）。下标从零开始
     */
    public Integer getSubtotalBeginRow()
    {
        return subtotalBeginRow;
    }

    
    /**
     * 设置：报表小计的开始行号（包括此行）。下标从零开始
     * 
     * @param subtotalBeginRow 
     */
    public void setSubtotalBeginRow(Integer i_SubtotalBeginRow)
    {
        this.subtotalBeginRow = i_SubtotalBeginRow;
        this.subtotalEndRow   = i_SubtotalBeginRow; 
    }

    
    /**
     * 获取：报表小计的结束行号（包括此行）。下标从零开始
     */
    public Integer getSubtotalEndRow()
    {
        return subtotalEndRow;
    }

    
    /**
     * 设置：报表小计的结束行号（包括此行）。下标从零开始
     * 
     * @param subtotalEndRow 
     */
    public void setSubtotalEndRow(Integer subtotalEndRow)
    {
        this.subtotalEndRow = subtotalEndRow;
    }


    /**
     * 获取：合计内容的开始行号（包括此行）。下标从零开始
     */
    public Integer getTotalBeginRow()
    {
        return totalBeginRow;
    }

    
    /**
     * 设置：合计内容的开始行号（包括此行）。下标从零开始
     * 
     * @param i_TotalBeginRow 
     */
    public void setTotalBeginRow(Integer i_TotalBeginRow)
    {
        this.totalBeginRow = i_TotalBeginRow;
        this.totalEndRow   = i_TotalBeginRow;
    }

    
    /**
     * 获取：合计内容的结束行号（包括此行）。下标从零开始
     */
    public Integer getTotalEndRow()
    {
        return totalEndRow;
    }

    
    /**
     * 设置：合计内容的结束行号（包括此行）。下标从零开始
     * 
     * @param totalEndRow 
     */
    public void setTotalEndRow(Integer totalEndRow)
    {
        this.totalEndRow = totalEndRow;
    }

    
    /**
     * 获取：报表数据的Java类型
     */
    public String getDataClass()
    {
        return dataClass;
    }

    
    /**
     * 设置：报表数据的Java类型
     * 
     * @param dataClass 
     */
    public void setDataClass(String dataClass)
    {
        this.dataClass = dataClass;
    }


    /**
     * 获取：值的标记。默认为一个冒号：":"
     */
    public String getValueSign()
    {
        return valueSign;
    }

    
    /**
     * 设置：值的标记。默认为一个冒号：":"
     * 
     * @param valueSign 
     */
    public synchronized void setValueSign(String valueSign)
    {
        this.valueSign = valueSign;
        
        this.valueNames.put(this.valueSign + $ValueName_RowNo            ,$ValueName_RowNo);
        this.valueNames.put(this.valueSign + $ValueName_RowIndex         ,$ValueName_RowIndex);
        this.valueNames.put(this.valueSign + $ValueName_RowCount         ,$ValueName_RowCount);
        this.valueNames.put(this.valueSign + $ValueName_RowSubtotalCount ,$ValueName_RowSubtotalCount);
    }

    
    /**
     * 获取：是要安全？还是要性能（默认为：性能）
     */
    public Boolean getIsSafe()
    {
        return isSafe;
    }


    /**
     * 设置：是要安全？还是要性能（默认为：性能）
     * 
     * @param isSafe 
     */
    public void setIsSafe(Boolean isSafe)
    {
        this.isSafe = isSafe;
    }

    
    /**
     * 获取：当为大数据量导出时，建议使用 SXSSFWorkbook
     * POI对excel的导出操作，一般只使用HSSFWorkbook以及SXSSFWorkbook，
     * HSSFWorkbook用来处理较少的数据量，
     * SXSSFWorkbook用来处理大数据量以及超大数据量的导出
     * 
     * 默认为：false
     */
    public Boolean getIsBig()
    {
        return isBig;
    }

    
    /**
     * 设置：当为大数据量导出时，建议使用 SXSSFWorkbook
     * POI对excel的导出操作，一般只使用HSSFWorkbook以及SXSSFWorkbook，
     * HSSFWorkbook用来处理较少的数据量，
     * SXSSFWorkbook用来处理大数据量以及超大数据量的导出
     * 
     * 默认为：false
     * 
     * @param isBig 
     */
    public void setIsBig(Boolean isBig)
    {
        this.isBig = isBig;
    }
    
    
    /**
     * 获取：当使用 SXSSFWorkbook 时，创建对象new SXSSFWorkbook(rowAccessWindowSize)入参。
     * 保持在内存中的行数，直到刷新为止
     */
    public Integer getRowAccessWindowSize()
    {
        return rowAccessWindowSize;
    }

    
    /**
     * 设置：当使用 SXSSFWorkbook 时，创建对象new SXSSFWorkbook(rowAccessWindowSize)入参。
     * 保持在内存中的行数，直到刷新为止
     * 
     * @param rowAccessWindowSize 
     */
    public void setRowAccessWindowSize(Integer rowAccessWindowSize)
    {
        this.rowAccessWindowSize = rowAccessWindowSize;
    }
    

    @Override
    public int compareTo(RTemplate i_Other)
    {
        if ( i_Other == null )
        {
            return 1;
        }
        else if ( this == i_Other )
        {
            return 0;
        }
        else
        {
            if ( this.excelFileName == null )
            {
                return -1;
            }
            
            return this.excelFileName.compareTo(i_Other.getExcelFileName());
        }
    }



    @Override
    public int hashCode()
    {
        if ( this.excelFileName != null )
        {
            return this.excelFileName.hashCode();
        }
        
        return super.hashCode();
    }



    @Override
    public boolean equals(Object i_Other)
    {
        if ( this == i_Other )
        {
            return true;
        }
        else if ( i_Other == null )
        {
            return false;
        }
        else if ( i_Other instanceof RTemplate )
        {
            if ( this.excelFileName == null )
            {
                return false;
            }
            
            return this.excelFileName.equals(((RTemplate)i_Other).getExcelFileName());
        }
        else
        {
            return false;
        }
    }



    @Override
    public String toString()
    {
        return Help.NVL(this.name ,Help.NVL(this.excelFileName));
    }
    
}
