package common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class Constants {
	public static int GRID_SIZE=100;         //空间大小
	public static int AMBULANCE_COUNT=10;   //急救车数量
	public static int CASUALTY_COUNT=150;   //伤员人数
	//public static Coordinate MCI_COR        //事件发生地点
	public static int HOSPITAL_COUNT=3;    //区域内医院数量
	
	public static int ED_AVAILABLE_COUNT=5;
	public static int ICU_AVAILABLE_COUNT=5;
	public static int GW_AVAILABLE_COUNT=5;
	public static int MCI_X=55;            //事件发生地点 坐标x
	public static int MCI_Y=25;            //事件发生地点坐标y
    public static String POSITION_BASE="Base"; //急救车位置标识  base表示其在基地
    public static String POSITION_INCIDENT="Incident";//急救车位置标识，Incident表示其在现场
    public static String POSITION_HOSPITAL="Hospital";//急救车位置标识，Hospital 表示其在医院
    public static String POSITION_ONTHEWAY="On_the_way";
    public static String POSITION_DISCHARGE="Discharge";
    public static String POSITION_DEAD="Dead";
    
    public static String HEADING_INCIDENT="Incident";
    public static String HEADING_HOSPITAL="Hospital";

	
	
	public static String CONTEXT_ID="mci";
	public static String SPACE_ID="space";
	public static String GRID_ID="grid";
	
	
	public static int AMBULANCE_VISION_RANGE=1;
	public static int INCIDENT_VISION_RANGE=1;
	
	public static String TAG_RED="Red";
	public static String TAG_YELLOW="Yellow";
	public static String TAG_GREEN="Green";
	public static String TAG_BLACK="Black";
    
	public static int TRIAGE_NUM_AT_ONE_TIME=2;
	
	public static int AMBULANCE_CARRY_MAX=2;
	
	public static double RPM_DET_RATE=0.01;//每一步，伤员RPM较少0.01
	
	public static int[][] rpmDeterationRecord=new int[22][14];//RPM随时间变化记录表
	
	
	public static int LOADCASUALTY_MODE=1;//0表示采用随机方式,1 表示按照RPM最小方式
	public static int LOADCASUALTY_MODE_0=0;
	public static int LOADCASUALTY_MODE_1=1;//选取 每个类型中 RPM最小的，即伤情最重的作为后送对象
	public static int LOADCASUALTY_MODE_2=2;//选取每个类型中RPM最大的，即伤情最轻的作为后送对象
	
	
	public static int CHOOSE_HOSPITAL_MODE=6;// 后送医院选择方式，0表示随机选择，1 表示按可用ED\ICU\GW数量最小原则选取医院，2
	public static int CHOOSE_HOSPITAL_MODE_0=0;//随机选择
	public static int CHOOSE_HOSPITAL_MODE_1=1;//当前医院资源使用数量最小
	public static int CHOOSE_HOSPITAL_MODE_2=2; //最短等待时间
	public static int CHOOSE_HOSPITAL_MODE_3=3;//后送医院选择方式，3 表示 根据伤员预测情况，对随机选择结果进行修正
	public static int CHOOSE_HOSPITAL_MODE_4=4;//zyh
	public static int CHOOSE_HOSPITAL_MODE_5=5;//就近 zyh
	public static int CHOOSE_HOSPITAL_MODE_6=6;// 使用matlab
	public static int DEPT_COUNT=3;//医院急诊包含部门数量，目前是 ED，icu，GW
	public static int DEPT_ED=0;   //ED 标号
	public static int DEPT_ICU=1;  //ICU 标号
	public static int DEPT_GW=2;   //GW 标号
	
	public static int HOSPITAL_ID=2;//输出医院ID对应的平均等待时间 0-hospital 0;1-hospital 1;2-hospital 2;
	
	
	public static double RUN_TIME=450.0;//系统运行时间
	
	public static double AMBULANCE_TRAVEL_SPEED=2.0;//急救车运行速度,
	public static double REPEAT_TIMES=1000;//一次实验的模拟次数
	
}
