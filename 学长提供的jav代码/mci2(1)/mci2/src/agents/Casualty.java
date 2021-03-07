package agents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import common.Constants;
import main.MCIContextBuilder;

public class Casualty {
	/*��ԱAgent
	 * 
	 * �¼������ص�   ��Ա��ʼλ��
	 * ��ԱΨһ���
	 * InitalRPM ��Ա��ʼ���飬RPMֵ0����12 ֮�������
	 * ��Ա������ʶ,red��RPM 1-4��,yellow(5-8),green(9-12),black(0)  //������ʶ�� �ֳ�����agent��ɣ�������Ա�����ɷ�����Ա������������λ������Ա����λʱ���ڵķ�������������һ����
	 * ������ʶ������ʱ��
	 * 
	 * ��Ա���ؼ��ȳ����          /���ȳ����Ｑ���ֳ��󣬵�һ�����ȳ�����Ա���ݷ�����Ա�������ĵ���ļ��ȳ���ɺ��ͣ�
	 *                   ���߼��ȳ�agent������ɺ��������ɷ���agent��ɷ����������ȥ�ط�����Ա��ε����ֳ��ģ�������ú���
	 * ��Ա���ؼ��ȳ�ʱ��         /���ȳ������ֳ��󣬰���װ�ع�������Ľ��������Ա�еĴ��ؼ��ȳ���Ÿ��輱�ȳ�Ψһ�ţ�
	 *                   �����ȳ��е���Ա�б��������Ӧ����Ա��ţ������ؾ�������֮����Ա���ؼ��ȳ�ʱ�����Լ�¼��ʱ��
	 * 
	 * ��Ա�������ҽԺʱ��      //�����ȳ�����Ա���͵�Ԥ�ڵ�ҽԺ�����ж�ED unit�������Ƿ񱥺ͣ����������Ա��Ҫ�ȴ�
	 * ��Ա����EDʱ��               //���ED unit����û�б�������Ա����ED������¼��Ա����ED��ʱ�̣�
	 *                    ����ED֮��ED unit����-1��Ȼ�������ԱRPMֵ�жϣ�
	 *                    RPM<=4��˵����Ա��Ҫ����������
	 *                           ��ʱ��Ҫ�ж�ICU/general ward bed count �Ƿ񱥺ͣ�������Ͳ�����ҪתԺ
	 *                                                                       ���δ���ͣ��ж�Operation_count�Ƿ񱥺� ���͵Ļ�תԺ
	 *                                                                                                        δ���͵Ļ���Ա���� Operation
	 *��Ա����ICU��ʱ��               4<RPM<=8 ˵����Ա��������ӳٴ���
	 *                           ��ʱ��Ҫ�ж�ICU/general ward bed count �Ƿ񱥺ͣ�������Ͳ��� ����ȴ�����
	 *                                                                         δ���ͣ����˽���ICU/GW
	 *                     RPM>8  ˵����Ա���ƽ��ᣬ��������֮����Ա��Ժ
	 *��Ա����GW��ʱ��                     
	 *                                                    
	 *                 
	 * 
	 * 
	 * 
	 * */
	public int casualtyID; //��ԱΨһ�Ա�ʶ
	public String casualtyPositionFlag; //��Աλ�ñ�ʶ����ʼ��ʱλ��Ϊ Incident��������ҽԺ֮��Ϊ On the way,����ҽԺ֮��Ϊ hospital
	public int RPM; //��Ա���飬��ʼ��ʱ���ղ�������
	public String triageTag;//��Ա������ʶ��red,yellow,black,green
	public int ambulanceID;//��Ա���صļ��ȳ�ID
	
	public double triageTime;//����ʱ��
	public double loadAmbulanceTime;//�����ȳ�����ʱ��
	public double arrHospitalTime;//����ҽԺʱ�䣬
	
	public double enterEDTime; //��Ա����ED��ʱ��
	public double enterICUTime;//��Ա����ICUʱ��
	public double leaveICUTime;//��Ա�뿪ICUʱ��
	public double enterGWTime; //��Ա����GW��ʱ��
	
	public double overICUTime;//��ԱICU�������ʱ��
	public double overGWTime;//��ԱGW�������ʱ��
	public double overEDTime;//��ԱED�������ʱ��
	public double leaveEDTime;//��ԱED�������ʱ��
	
	public double dischargeTime;//��Ա��Ժʱ��
	
	public double expectSurTime;//��ԱԤ�ڵ�����ʱ��
	//static ISchedule schedule;//ʱ���¼��Ҫ
	
	public String deadPosition;//��¼��Ա�����ص�
	public double deadTime;//��¼��Ա����ʱ��
	public boolean valuableOfRevise = false;//��Ա�м�ֵת�ˣ�����;�йҵ���
	public boolean used = false; //ת����ʱʹ��

	
	public Casualty(int id,int rpm){
		this.casualtyID=id; //��ʼ����ֵ����ԱID
		casualtyPositionFlag=Constants.POSITION_INCIDENT; //��ʼλ��
		this.InitialRPM=rpm; //��ʼ����ֵ
		this.RPM=rpm; //��ʼ����ֵ����Ա����RPM�������ض��ֲ����Ȱ���̫�ֲ�����
		this.triageTag=null;
		this.stopDeterioration=false;//��ʼ�����£���ʶ��Ա���鲻ͣ��
	//	this.expectSurTime=suvivalTimeCount(rpm);//���ݳ�ʼrpm������ԱԤ�ڵ�����ʱ�䡣
		
	}
	public void step(){

		double currentTime = MCIContextBuilder.currentTime;
		//System.out.println("currentTime "+currentTime);
		if(this.RPM<=0){
			this.stopDeterioration=true;
//			String out="Casualty "+this.casualtyID+" dead "+"at "+currentTime+" at "+this.casualtyPositionFlag;
//			System.out.println(out);
			if(this.deadTime ==0.0){ //���ڻ�û�м�¼������Ϣ����Ա����¼�����ص㣬������ʱ�䣬�����Ѿ���¼���ģ������ٴμ�¼
				String out="Casualty "+this.casualtyID+" dead "+"at "+currentTime+" at "+this.casualtyPositionFlag;
				System.out.println(out);
				this.deadPosition=this.casualtyPositionFlag;//��������ʱ���ȼ�¼������λ��
				this.deadTime=currentTime;//��¼����Ա����ʱ��
				this.casualtyPositionFlag=Constants.POSITION_DEAD; //�����Ա��������λ�ø�ֵΪdead
			}
			
			//die();
		}else{
			
			//��ԱRPM���٣����ݳ�ʼֵ�����ٱ�����ͬ
			if(!this.stopDeterioration){//�񻯱�ʶΪfalse,��ʶ��Ա����һֱ��
				this.RPM=getCurrentRPM(currentTime,this.InitialRPM);
				//System.out.println("currentrpm "+this.RPM);
			}else
			{//�����ֹͣ,����RPM�����޸�
				
			}
		
		}
	}
	
//	private void die(){
//		
//		ContextUtils.getContext(this).remove(this);
//		
//	}
	/*��������ʱ�䳤�̣��ͳ�ʼRPM��������Ա���ڵ�RPM*/
	private int getCurrentRPM(double t,int iniRPM){
		int cRPM=0;		
		for(int aa=0;aa<Constants.rpmDeterationRecord.length-1;++aa){
			if((int)t>=Constants.rpmDeterationRecord[aa][0]){
				for(int bb=1;bb<Constants.rpmDeterationRecord[aa].length;++bb){
					if(iniRPM==Constants.rpmDeterationRecord[0][bb]){
						cRPM=Constants.rpmDeterationRecord[aa][bb];
					}
				}
				
			}
		}
		
		return cRPM;
	}

}
