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
	public static int GRID_SIZE=100;         //�ռ��С
	public static int AMBULANCE_COUNT=10;   //���ȳ�����
	public static int CASUALTY_COUNT=150;   //��Ա����
	//public static Coordinate MCI_COR        //�¼������ص�
	public static int HOSPITAL_COUNT=3;    //������ҽԺ����
	
	public static int ED_AVAILABLE_COUNT=5;
	public static int ICU_AVAILABLE_COUNT=5;
	public static int GW_AVAILABLE_COUNT=5;
	public static int MCI_X=55;            //�¼������ص� ����x
	public static int MCI_Y=25;            //�¼������ص�����y
    public static String POSITION_BASE="Base"; //���ȳ�λ�ñ�ʶ  base��ʾ���ڻ���
    public static String POSITION_INCIDENT="Incident";//���ȳ�λ�ñ�ʶ��Incident��ʾ�����ֳ�
    public static String POSITION_HOSPITAL="Hospital";//���ȳ�λ�ñ�ʶ��Hospital ��ʾ����ҽԺ
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
	
	public static double RPM_DET_RATE=0.01;//ÿһ������ԱRPM����0.01
	
	public static int[][] rpmDeterationRecord=new int[22][14];//RPM��ʱ��仯��¼��
	
	
	public static int LOADCASUALTY_MODE=1;//0��ʾ���������ʽ,1 ��ʾ����RPM��С��ʽ
	public static int LOADCASUALTY_MODE_0=0;
	public static int LOADCASUALTY_MODE_1=1;//ѡȡ ÿ�������� RPM��С�ģ����������ص���Ϊ���Ͷ���
	public static int LOADCASUALTY_MODE_2=2;//ѡȡÿ��������RPM���ģ��������������Ϊ���Ͷ���
	
	
	public static int CHOOSE_HOSPITAL_MODE=6;// ����ҽԺѡ��ʽ��0��ʾ���ѡ��1 ��ʾ������ED\ICU\GW������Сԭ��ѡȡҽԺ��2
	public static int CHOOSE_HOSPITAL_MODE_0=0;//���ѡ��
	public static int CHOOSE_HOSPITAL_MODE_1=1;//��ǰҽԺ��Դʹ��������С
	public static int CHOOSE_HOSPITAL_MODE_2=2; //��̵ȴ�ʱ��
	public static int CHOOSE_HOSPITAL_MODE_3=3;//����ҽԺѡ��ʽ��3 ��ʾ ������ԱԤ������������ѡ������������
	public static int CHOOSE_HOSPITAL_MODE_4=4;//zyh
	public static int CHOOSE_HOSPITAL_MODE_5=5;//�ͽ� zyh
	public static int CHOOSE_HOSPITAL_MODE_6=6;// ʹ��matlab
	public static int DEPT_COUNT=3;//ҽԺ�����������������Ŀǰ�� ED��icu��GW
	public static int DEPT_ED=0;   //ED ���
	public static int DEPT_ICU=1;  //ICU ���
	public static int DEPT_GW=2;   //GW ���
	
	public static int HOSPITAL_ID=2;//���ҽԺID��Ӧ��ƽ���ȴ�ʱ�� 0-hospital 0;1-hospital 1;2-hospital 2;
	
	
	public static double RUN_TIME=450.0;//ϵͳ����ʱ��
	
	public static double AMBULANCE_TRAVEL_SPEED=2.0;//���ȳ������ٶ�,
	public static double REPEAT_TIMES=1000;//һ��ʵ���ģ�����
	
}
