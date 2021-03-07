package agents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mathworks.toolbox.javabuilder.*;

//import com.mathworks.toolbox.javabuilder.MWArray;
//import com.mathworks.toolbox.javabuilder.MWClassID;
//import com.mathworks.toolbox.javabuilder.MWException;
//import com.mathworks.toolbox.javabuilder.MWNumericArray;

import common.Constants;
import main.MCIContextBuilder;
import agents.Ambulance;
import agents.Hospital;
import agents.Casualty;
import testing1.mci2;


public class Incident {
	public int x, y; // �¹ʷ����ص�����
	private boolean triageStart; // ������ʼ��ʶ������һ�����ȳ�����ʱ���ſ�ʼ������Ա����������


	// ��Ա��Ϣ��������������ص����
	private int survivalNum; // ��ǰ�Ҵ�����
	private int deadNum;// ��ǰ��������
	private int beTriagedNum; // ����������
	private int onIncident; // ���ڼ����ֳ�����Ա
	private int onTheWaydNum; // �ں���;������
	private int onTheHospitaldNum; // ��ǰ��Ժ������
	private int dischargeNum;// ��ǰ��Ժ����
	private int[] atHospitaldNum; // ����ҽԺ����,����ҽԺID�ֲ�

	private double[] hospitalWaitEDTime;// ����ҽԺED�ȴ�ʱ�䳤��
	private double[] hospitalWaitICUTime;// ����ҽԺicu�ȴ�ʱ�䳤��
	private double[] hospitalWaitGWTime;// ����ҽԺGW�ȴ�ʱ�䳤��

	private int[] hospitalEDAvailableCount;// ����ҽԺED������Դ
	private int[] hospitalICUAvailableCount;// ����ҽԺICU������Դ
	private int[] hospitalGWAvailableCount;// ����ҽԺGW������Դ
	
	private double[][] hospitalTreatTime;// ����ҽԺED����ʱ�䳤�� zyh  ͳ����
//	private double[] hospitalTreatICUTime;// ����ҽԺicu����ʱ�䳤��
//	private double[] hospitalTreatGWTime;// ����ҽԺGW����ʱ�䳤��

	public List<Casualty> casualtyOnBoard;
	
	public Incident(int x, int y) {
		if (x < 0) {
			throw new IllegalArgumentException(String.format(
					"Coordinate x=%d<0.", x));
		}
		if (y < 0) {
			throw new IllegalArgumentException(String.format(
					"Coordinate y=%d<0.", y));
		}
		this.x = x;
		this.y = y;
		this.triageStart = false;

		this.survivalNum = 0;// ����������������ʼֵΪ0��
		this.beTriagedNum = 0;
		this.onIncident = 0;
		this.onTheWaydNum = 0;
		this.onTheHospitaldNum = 0;
		this.dischargeNum = 0;
		this.atHospitaldNum = new int[Constants.HOSPITAL_COUNT];
	}
	
	public void step() {

		double currentTime = MCIContextBuilder.currentTime;

		// ��ȡ�¼��ص��Ƿ��м��ȳ�����

		List<Ambulance> ambulanceArrived = new ArrayList<Ambulance>();
		List<Ambulance> bestAmbulanceList = new ArrayList<Ambulance>();  //��ѾȻ������ zyh
		for (int a=0; a<MCIContextBuilder.ambulanceList.size(); ++a) {			
			Ambulance cc = MCIContextBuilder.ambulanceList.get(a);									

			if (cc.positionFlag == Constants.POSITION_INCIDENT) {
				if (cc.casualtyList == null) { // �Ե����ֳ�֮��û��װ�ز��˵ļ��ȳ����У�װ�ز�����
					ambulanceArrived.add(cc);
				} else if (cc.casualtyList.size() == 0) {
					ambulanceArrived.add(cc);
				}

			}
		}

		if (ambulanceArrived.size() == 0) { // ��ʾû�п���װ�صļ��ȳ�������û�е����
			if (triageStart) { // �ж��Ƿ�ʼ������true��ʱ�򣬽��з�������������������жϵĻ�����
				setTriageTag();
			} else {
				// û�п�ʼ������ʲô��������
			}
		} else {// ��ʾ�м��ȳ�����
			System.out.println("�ֳ��г�"+ambulanceArrived.size());
			Integer remain = 0;
			if (triageStart) {
				remain = setTriageTag();
			} else {
				triageStart = true;// �ж��ǲ��ǵ�һ��������ǵ�һ��������־λ��Ϊtrue��
			}
			this.casualtyOnBoard = loadCasualty(ambulanceArrived);               //ѡȡ��Ա
			//System.out.println("cas selected");
			//System.out.println(this.casualtyOnBoard.size());
			if(this.casualtyOnBoard.size()>0) {                                  //�������Ա
				if(this.casualtyOnBoard.size()<Constants.AMBULANCE_CARRY_MAX*ambulanceArrived.size() && remain>0) {   //��������������������δtag�����⳵���˷ѣ�
					ambulanceArrived = ambulanceArrived.subList(0, (int) Math.ceil(this.casualtyOnBoard.size()/2));
				}
			bestAmbulanceList=new ArrayList<Ambulance>(allocateCas(this.casualtyOnBoard,ambulanceArrived));  //�����Աȡ������
			//System.out.println("681 "+bestAmbulanceList.size());
			for(int ii=0; ii<bestAmbulanceList.size();++ii) {                  //����
				Ambulance one = bestAmbulanceList.get(ii);
				//System.out.println("685: "+one.casualtyList.size());

					Ambulance cc = (Ambulance) ambulanceArrived.get(ii);
					//System.out.println("688: "+cc.casualtyList.size());

					if (cc.positionFlag == Constants.POSITION_INCIDENT) {                        //���ֳ��ĳ�
						if (cc.casualtyList == null) { // �Ե����ֳ�֮��û��װ�ز��˵ļ��ȳ����У�װ�ز�����
							cc.casualtyList = one.casualtyList;
							//System.out.println("a car loaded");
						} else if (cc.casualtyList.size() == 0) {
							cc.casualtyList = one.casualtyList;
							//System.out.println("b car loaded");
						}
						if(cc.casualtyList.size()>0) {
							//System.out.println("a car leaving");
							cc.target_hospital = one.target_hospital;
							String out="Ambulance "+cc.ambulanceID+" travel to"+" Hospital "+cc.target_hospital.hid;
							for(int jj=0;jj<cc.casualtyList.size();++jj){
								out+=" Casualty "+cc.casualtyList.get(jj).casualtyID+" ";
								cc.casualtyList.get(jj).casualtyPositionFlag=Constants.POSITION_ONTHEWAY;//�޸���Աλ����ϢΪ��·��
								cc.casualtyList.get(jj).loadAmbulanceTime=currentTime;//��¼��Ա�������ϼ��ȳ���ʱ��
								cc.casualtyList.get(jj).ambulanceID=cc.ambulanceID;//��¼��Ա���صļ��ȳ����
							}
							System.out.println(out);
							cc.targetx=cc.target_hospital.x;
							cc.targety=cc.target_hospital.y;
							cc.positionFlag=Constants.POSITION_ONTHEWAY;//�����ȳ�״̬��Ϊ ��·��	���������ȳ�
						}else {
							cc.positionFlag=Constants.POSITION_INCIDENT;
							System.out.println("Ambulance "+cc.ambulanceID+" wait at Incident");//���
						}

					}
				
				
			}
			}
		}
		
		 if(currentTime==Constants.RUN_TIME){
		// �����Աͳ������
		// �����Ա��Ϣ����csv�ļ�
		// ����ÿһ������ǰ���Ҵ���������ǰ�ֳ��ѷ�����������ǰ����;����������ǰ��Ժ����������ǰ��ҽԺ��Ժ��������ǰ��Ժ����
			 for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
					Casualty oneCasualty = MCIContextBuilder.casualtyList.get(a);// �����е���Ա������б��������
				// this.survivalNum++;// ������һ����Ա���󣬱����仹����������������+1
				if ((oneCasualty.casualtyPositionFlag == Constants.POSITION_INCIDENT)
						&& (oneCasualty.triageTag != null)) {
					this.beTriagedNum++;// �ֳ��ѷ�������
				}
				if (oneCasualty.casualtyPositionFlag == Constants.POSITION_INCIDENT) {
					this.onIncident++;
				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_ONTHEWAY) {// ��Ա�Ѿ���·���ˣ���¼������
					this.onTheWaydNum++;

				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// ��Ա����ҽԺ
					this.atHospitaldNum[oneCasualty.arrHospitalID]++;// ������Ա���������ҽԺ������ҽԺ��Ӧ����Ա����+1��
				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_DISCHARGE) {// ��ʾ��Ա�Ѿ���Ժ
					this.dischargeNum++;
				} else {// ��Ա������
					this.deadNum++;//
				}
			}
		

		for (int ww = 0; ww < this.atHospitaldNum.length; ++ww) {
			this.onTheHospitaldNum += this.atHospitaldNum[ww];
		}
		this.survivalNum = Constants.CASUALTY_COUNT - this.deadNum;// ���㵱ǰ�Ҵ�����

		try {
			File csv = new File("D:\\eclipse-workspace\\mci\\data\\casualty0824.csv");
			BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
			// bw.write("��ǰʱ��"+","+"��ǰ�Ҵ�����"+","+"��ǰ�ֳ�����"+"��ǰ�ѷ�������"+","+"��ǰ����;������"+","+"��ǰ��Ժ������"+"\r\n");
			bw.newLine();
			String outline = currentTime + "," + this.survivalNum + ","
					+ this.onIncident + "," + this.beTriagedNum + ","
					+ this.onTheWaydNum + "," + this.onTheHospitaldNum + ","
					+ this.dischargeNum;
			bw.write(outline);
			bw.close();
			// ÿһ�����֮����Ҫ������������㣬�����´μ���
			this.survivalNum = 0;// ����������������ʼֵΪ0��
			this.deadNum = 0;
			this.beTriagedNum = 0;
			this.onIncident = 0;
			this.onTheWaydNum = 0;
			this.onTheHospitaldNum = 0;
			this.dischargeNum = 0;
			this.atHospitaldNum = new int[Constants.HOSPITAL_COUNT];

			// osw.close();
			// out.close();

		} catch (Exception e) {
			throw new IllegalArgumentException(String.format(e.toString()));

		}

//		// ������Ա��ҽԺ�ĵȴ�ʱ��
//		for (Object obj : getGrid().getObjects()) {
//			if (obj instanceof Casualty) {
//				Casualty aCasualty = (Casualty) obj;
//				if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// ͳ�Ƶ�ǰ��Ա��ҽԺ�ĵȴ�ʱ��
//					double waitEDTime = 0.0; // ��ʼֵΪ0.0
//					double waitICUTime = 0.0; // ��ʼֵΪ0.0
//					double waitGWTime = 0.0; // ��ʼֵΪ0.0
//
//					if (aCasualty.enterEDTime != 0.0) {// �����Ա����ED�����enterEDTime
//														// Ϊ0.0
//														// ��ʾ��Ա�����˵ȴ��б���ʱ�ݲ�����ED�ȴ�ʱ��
//						waitEDTime = aCasualty.enterEDTime
//								- (aCasualty.arrHospitalTime);// ��Ա�ȴ�����ED��ʱ��
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime != 0.0)
//							&& ((aCasualty.enterGWTime == 0.0))) {// ��ʾ��Ա����ICU����û�н���GW
//						// ����ȴ�����ICU��ʱ��
//						waitICUTime = aCasualty.enterICUTime
//								- aCasualty.enterEDTime;
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime == 0.0)
//							&& ((aCasualty.enterGWTime != 0.0))) {// ��ʾ��Ա����GW��û�н���ICU
//						// ���� ��ED�ȴ�����GW��ʱ��
//						waitGWTime = aCasualty.enterGWTime
//								- aCasualty.enterEDTime;
//
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime != 0.0)
//							&& ((aCasualty.enterGWTime != 0.0))) {// ��ʾ��Ա�Ƚ���ICU,Ȼ���ٽ���GW
//						// ���� ��ICU�ȴ�����GW��ʱ��
//						waitGWTime = aCasualty.enterGWTime
//								- aCasualty.leaveICUTime;
//					}
//
//					// Ϊ����Ա����ҽԺ����� �ȴ�ʱ��
//					this.hospitalWaitEDTime[aCasualty.arrHospitalID] += waitEDTime;
//					this.hospitalWaitICUTime[aCasualty.arrHospitalID] += waitICUTime;
//					this.hospitalWaitGWTime[aCasualty.arrHospitalID] += waitGWTime;
//				}
//			}
//		}
//		// �������ҽԺ�ļ�����������Ҫ��ED count,icu_count ��GW_count
//		for (Object obj : getGrid().getObjects()) {// ѡȡҽԺ����,������ǰ�ĸ�����Դ�������ֵ������
//			if (obj instanceof Hospital) {
//				Hospital aHospital = (Hospital) obj;
//				this.hospitalEDAvailableCount[aHospital.hid] = aHospital.ED_Available_Count;
//				this.hospitalICUAvailableCount[aHospital.hid] = aHospital.ICU_Avaible_Bed_Count;
//				this.hospitalGWAvailableCount[aHospital.hid] = aHospital.GW_Avaible_Bed_Count;
//			}
//		}

//		// �������ҽԺ�ļ��������͵ȴ�ʱ��
//		try {
//			File csv = new File("D:\\eclipse-workspace\\mci\\data\\hospital.csv");
//			BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
//
//			String oneLine = currentTime + ",";
//			bw.newLine();
//			for (int ww = 0; ww < Constants.HOSPITAL_COUNT; ++ww) {
//
//				oneLine += this.hospitalEDAvailableCount[ww] + ","
//						+ this.hospitalWaitEDTime[ww] + ","
//						+ this.hospitalICUAvailableCount[ww] + ","
//						+ this.hospitalWaitICUTime[ww] + ","
//						+ this.hospitalGWAvailableCount[ww] + ","
//						+ this.hospitalWaitGWTime[ww] + ",";
//			}
//			bw.write(oneLine);
//			bw.close();
//			// ÿһ�����֮����Ҫ������������㣬�����´μ���
//			hospitalWaitEDTime = new double[Constants.HOSPITAL_COUNT];// ����ҽԺED�ȴ�ʱ�䳤��
//			hospitalWaitICUTime = new double[Constants.HOSPITAL_COUNT];// ����ҽԺicu�ȴ�ʱ�䳤��
//			hospitalWaitGWTime = new double[Constants.HOSPITAL_COUNT];// ����ҽԺGW�ȴ�ʱ�䳤��
//
//		} catch (Exception e) {
//			throw new IllegalArgumentException(String.format(e.toString()));
//
//		}
		
//		//ͳ�ƴ���ʱ�� zyh
//		 this.hospitalTreatTime=getAvgTreatTime();
//		// �������ҽԺ�ļ������� zyh
//		try {
//			File csv = new File("D:\\eclipse-workspace\\mci\\data\\hospital.csv");
//			BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
//
//			String oneLine = currentTime + ",";
//			bw.newLine();
//			for (int ww = 0; ww < Constants.HOSPITAL_COUNT; ++ww) {
//
//				oneLine += 						
//						 this.hospitalTreatTime[ww][Constants.DEPT_ED] + ","						
//						+ this.hospitalTreatTime[ww][Constants.DEPT_ICU] + ","						
//						+ this.hospitalTreatTime[ww][Constants.DEPT_GW] + ",";
//			}
//			bw.write(oneLine);
//			bw.close();			
//
//		} catch (Exception e) {
//			throw new IllegalArgumentException(String.format(e.toString()));
//
//		}
	}

	}
	
	private List<Casualty> loadCasualty(List<Ambulance> ambulanceArrived) {
		// ���ȳ�������Ա����д�ڴ˴������뼱�ȳ�����������ص���Ա�б�
		// ѡ�������Ѿ�tag����Ա
		// List<Object> casualtiesHaveTag=new ArrayList<Object>();
		List<Casualty> tempCasualtyList = new ArrayList<Casualty>();

		List<Casualty> casualtiesTagRed = new ArrayList<Casualty>();
		List<Casualty> casualtiesTagYellow = new ArrayList<Casualty>();
		List<Casualty> casualtiesTagGreen = new ArrayList<Casualty>();
		for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
			Casualty mm = MCIContextBuilder.casualtyList.get(a);
				if (mm.casualtyPositionFlag == Constants.POSITION_INCIDENT) {
					if (mm.triageTag == Constants.TAG_RED) {
						casualtiesTagRed.add(mm);
					}
					if (mm.triageTag == Constants.TAG_YELLOW) {
						casualtiesTagYellow.add(mm);
					}
					if (mm.triageTag == Constants.TAG_GREEN) {
						casualtiesTagGreen.add(mm);
					}

			}
		}
		//for(int i =0; i<amblist.size();++i) {

		int temp = 0;
		do {
			if(casualtiesTagRed.size()>0){
				//��ʾ�ֳ����ں�ɫ��Ա�����ȼ���
				int index=0;	
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagRed.size());//��CasualtiesRed�����ѡȡһ����Ա���������ȡ�����ʽ�����ú��ַ�ʽѡ����Ա�������������
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagRed);// ѡȡRPM��С�� ���� ��Ϊ����		
				}
				Object obj=casualtiesTagRed.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //��Ա����֮��,����λ����Ϣ�޸�Ϊ��·��
				casualtiesTagRed.remove(index); //��Ա��װ��֮�󽫸���Ա�Ӻ�ɫ�б������ߣ�
				temp++;
			}else if(casualtiesTagYellow.size()>0){
				int index=0;		
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagYellow.size());//��CasualtiesRed�����ѡȡһ����Ա���������ȡ�����ʽ�����ú��ַ�ʽѡ����Ա�������������
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagYellow);// ѡȡRPM��С�� ���� ��Ϊ����	
				}
				Object obj=casualtiesTagYellow.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //��Ա����֮��,����λ����Ϣ�޸�Ϊ��·��
				casualtiesTagYellow.remove(index); //��Ա��װ��֮�󽫸���Ա�ӻ�ɫ�б������ߣ�
				temp++;
			}else if(casualtiesTagGreen.size()>0){
				int index=0;				
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagGreen.size());//��CasualtiesRed�����ѡȡһ����Ա���������ȡ�����ʽ�����ú��ַ�ʽѡ����Ա�������������
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagGreen);// ѡȡRPM��С�� ���� ��Ϊ����	
				}
				Object obj=casualtiesTagGreen.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //��Ա����֮��,����λ����Ϣ�޸�Ϊ��·��
				casualtiesTagGreen.remove(index); //��Ա��װ��֮�󽫸���Ա����ɫ�б������ߣ�
				temp++;
			}else{
				temp++;// �ֳ�û�п���װ�ص���Ա��
			}

		} while (temp < Constants.AMBULANCE_CARRY_MAX*ambulanceArrived.size());
		//}
		return tempCasualtyList;

	}
	private int newChooseCasualtyIndexBasedOnRPM(List<Casualty> casualtyList){
		int index=0;//���ص�����ֵ
		int minRPM=12;//��СRPM�ĳ�ʼֵ
			
		//ɸѡ�м�ֵ��Ա
		for(int i=0;i<casualtyList.size();++i){
			Casualty one=casualtyList.get(i);//��obj����תΪCasualty����
			one.valuableOfRevise = false;
			int[] expectRPM=new int[Constants.HOSPITAL_COUNT]; //������� expectRPM[��amb�е���Ա��ţ�������ԱcasualtyID][ҽԺID]
			
			for(int j=0;j<MCIContextBuilder.hospitalList.size();++j){
				Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//��ҽԺ������ѡȡһ�� ҽԺ����
				//expectRPM[oneHospital.hid]=newExpectRPMOnArriveHos(one,oneHospital);//���� Ԥ�ڵ�RPM
				expectRPM[oneHospital.hid]=newExpectRPM(one,oneHospital);//���� Ԥ�ڵ�RPM
				if(expectRPM[oneHospital.hid]>0) {
					one.valuableOfRevise = true;
					break;
				}
			}
			if((one.RPM<minRPM) &&(one.valuableOfRevise = true)){//��ĳһ�������RPMС����СRPMֵʱ������ֵ��ֵ��minRPM  ֻѡ�񲻻�;��������
				//if(one.RPM<minRPM){//��ĳһ�������RPMС����СRPMֵʱ������ֵ��ֵ��minRPM  ֻѡ�񲻻�;��������
				minRPM=one.RPM;
				index=i;//ͬʱ����ʱ������ֵ��ֵ�� index ���ڷ���
			}
		}
		return index;
	}
	private List<Ambulance> allocateCas(List<Casualty> casualtyOnBoard2, List<Ambulance> ambulanceArrived) {
		List<Casualty> casualtylist = casualtyOnBoard2;
		List<Ambulance> ambulanceList = new ArrayList<Ambulance>();
		List<Ambulance> bestAmbulanceList = new ArrayList<Ambulance>();  //��ѾȻ������
		List<Casualty> onelist = new ArrayList<Casualty>();
		List<Casualty> twolist = new ArrayList<Casualty>();
		List<Casualty> threelist = new ArrayList<Casualty>();
		List<Casualty> fourlist = new ArrayList<Casualty>();
		List<Casualty> fivelist = new ArrayList<Casualty>();
		List<Casualty> sixlist = new ArrayList<Casualty>();
		List<Casualty> sevenlist = new ArrayList<Casualty>();
		List<Casualty> eightlist = new ArrayList<Casualty>();
		List<Casualty> ninelist = new ArrayList<Casualty>();
	
		
		//��ȡҽԺ����
		List<Hospital> hospitalList=MCIContextBuilder.hospitalList;	
		int index=0;
		int hospitalUsedCountList[]=new int[Constants.HOSPITAL_COUNT];//ҽԺʹ�����б� 
		int hospitalEDUsedCountList[]=new int[Constants.HOSPITAL_COUNT];//ҽԺ������ʹ�����б�
		for(int c=0; c<hospitalList.size(); ++c){
				Hospital tt=hospitalList.get(c);
				//ȡ����ǰ����ҽԺ ����Ĵ�λʹ������
				int usedCount=(Constants.ED_AVAILABLE_COUNT+Constants.ICU_AVAILABLE_COUNT+Constants.GW_AVAILABLE_COUNT)-(tt.ED_Available_Count+tt.ICU_Avaible_Bed_Count+tt.GW_Avaible_Bed_Count);
				//����ǰ ���崲λ���� ���浽���� ��ӦҽԺ�������
				hospitalUsedCountList[tt.hid]=usedCount;
				hospitalEDUsedCountList[tt.hid]=tt.ED_Available_Count;
			
		}
		
		if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_0){ //���ѡ��ģʽ
	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);//��obj����תΪAmbulance����
				t.casualtyList = new ArrayList<Casualty>();
				for(int jj=0; jj<Constants.AMBULANCE_CARRY_MAX;++jj) {
				if(casualtylist.size()>0) {
					Random r = new Random();
					int k=r.nextInt(casualtylist.size());
					Casualty cc = casualtylist.get(k);
					t.casualtyList.add(cc);
					casualtylist.remove(cc);
				}
				}
				//���ѡ��һ��ҽԺ��Ϊ����ҽԺ��
				Random r = new Random();
				index=r.nextInt(hospitalList.size());
				Hospital tHospital=(Hospital)hospitalList.get(index);
				t.target_hospital = tHospital;
				bestAmbulanceList.add(t);
			}
		}
		else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_1) { //���ô�λ���ܺ���С
			int min=hospitalUsedCountList[0];
			for(int i=0;i<hospitalUsedCountList.length;++i){
				if(min>hospitalUsedCountList[i]){
					min=hospitalUsedCountList[i];
					index=i;			
				}
			}

			//ѡȡҽԺ��hidΪindex�ģ���Ϊ���index�����ҽԺ��hid����
			
			List<Hospital> hospitalResult=new ArrayList<Hospital>();
			for(int c=0; c<hospitalList.size(); ++c){
				Hospital oneHospital=hospitalList.get(c);
					if(oneHospital.hid==index){
						hospitalResult.add(oneHospital);						
					}
			}
				
			Hospital tHospital=hospitalResult.get(0);	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);
				t.casualtyList = new ArrayList<Casualty>();
				for(int jj=0; jj<Constants.AMBULANCE_CARRY_MAX;++jj) {
				if(casualtylist.size()>0) {
					Random r = new Random();
					int k=r.nextInt(casualtylist.size());
					Casualty cc = casualtylist.get(k);
					t.casualtyList.add(cc);
					casualtylist.remove(cc);
				}
				}
				t.target_hospital = tHospital;
				bestAmbulanceList.add(t);
			}
		}
			else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_2) {	//���п���ƽ���ȴ�ʱ�����
			double[][] avgWaitTime=getAvgWaitTime();//��ȡ��ǰÿ��ҽԺ��ÿ�����ҵ�ƽ���ȴ�ʱ��
			double minAvgWaitTime=avgWaitTime[0][Constants.DEPT_ED]+avgWaitTime[0][Constants.DEPT_ICU]+avgWaitTime[0][Constants.DEPT_GW];
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				if(minAvgWaitTime>(avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW])){
					index=i;//����С�� ҽԺID��ֵ��index
					minAvgWaitTime=avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW];
				}
			}
			
			//ѡȡҽԺ��hidΪindex�ģ���Ϊ���index�����ҽԺ��hid����			
			List<Hospital> hospitalResult=new ArrayList<Hospital>();
			for(int c=0; c<hospitalList.size(); ++c){
				Hospital oneHospital=hospitalList.get(c);
					if(oneHospital.hid==index){
						hospitalResult.add(oneHospital);						
					}
				}	
			Hospital tHospital=(Hospital)hospitalResult.get(0);	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);
				t.casualtyList = new ArrayList<Casualty>();
				for(int jj=0; jj<Constants.AMBULANCE_CARRY_MAX;++jj) {
				if(casualtylist.size()>0) {
					Random r = new Random();
					int k=r.nextInt(casualtylist.size());
					Casualty cc = casualtylist.get(k);
					t.casualtyList.add(cc);
					casualtylist.remove(cc);
				}
				}
				t.target_hospital = tHospital;
				bestAmbulanceList.add(t);
			}
		}
			else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_5) { //��̾���
			List<Hospital> hospitalResult=new ArrayList<Hospital>();
			for(int i=0;i<hospitalEDUsedCountList.length;++i){
				if(hospitalEDUsedCountList[i]>0){
					index=i;	
					for(int c=0; c<hospitalList.size(); ++c){     //������м������пմ�λ��
						Hospital oneHospital=hospitalList.get(c);
							if(oneHospital.hid==index){
								hospitalResult.add(oneHospital);						
							}
					}
				}
			}
			double min = 1000;
			int index2=0;
			if(hospitalResult!=null && hospitalResult.size()>0) { //�м����пմ�λ��ҽԺ
				
				for(int i=0;i<hospitalResult.size();++i){     //�пմ�λ�����ҽԺ
					Hospital hospitalPosition=hospitalResult.get(i);//��ȡҽԺ����λ��
					double dis=Math.abs(Constants.MCI_X-hospitalPosition.x)+Math.abs(Constants.MCI_Y-hospitalPosition.y);  //���� ��Ա��ҽԺ֮��ľ���					
					if (dis<min) {
						min=dis;
						index2=i;				
					}			
				}			
			}else { //û�������ѡ
				Random r = new Random();
				index2=r.nextInt(hospitalResult.size());
			}
										
			Hospital tHospital=hospitalResult.get(index2);	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);//��obj����תΪAmbulance����
				t.casualtyList = new ArrayList<Casualty>();
				for(int jj=0; jj<Constants.AMBULANCE_CARRY_MAX;++jj) {
				if(casualtylist.size()>0) {
					Random r = new Random();
					int k=r.nextInt(casualtylist.size());
					Casualty cc = casualtylist.get(k);
					t.casualtyList.add(cc);
					casualtylist.remove(cc);
				}
				}
				
				t.target_hospital = tHospital;
				bestAmbulanceList.add(t);
			}
			
			
			
		}
//		else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_4) {
//		int bestRPM = 0;                                      //���RPM
//		//for(int i=0;i<ambulanceArrived.size();++i){
//		if(ambulanceArrived.size()==1) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);
//			one.casualtyList=casualtylist;
//			one.target_hospital = chooseHospitalBasedOnRPM(one.casualtyList).hos;
//			bestAmbulanceList.add(one);
//		}
//		if(ambulanceArrived.size()==2) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = true;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = true;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = true;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = true;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//		}
//		
//		if(ambulanceArrived.size()==3) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
//				//System.out.println("1035: "+s.casualtyID);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							System.out.println("1043: "+s.casualtyID);
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//					//System.out.println("1053: "+s.casualtyID);
//				one.casualtyList.add(s);
//				s.used = true;
//				//System.out.println("1055: "+one.casualtyList.size());
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
//					//System.out.println("1056: "+ss.casualtyID);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						System.out.println("1063: "+ii);
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							System.out.println("1067: "+jj);
////							System.out.println("1064: "+cc.casualtyID);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//						//System.out.println("1075: "+ss.casualtyID);
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//							
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								//bestAmbulanceList = new ArrayList<Ambulance>();
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							
//							
//							
//							
//							
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						//System.out.println("1212: "+ss.casualtyID);
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					//System.out.println("1217: "+s.casualtyID);
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			
//			
//		}
//		if(ambulanceArrived.size()==4) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							
//						
//							
//							
//							
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//		}
//		if(ambulanceArrived.size()==5) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//											for(int i5=0;i5<casualtylist.size();++i5){//��һ������ 5��
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////													if(t.casualtyList!=null) {
////													for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////														Casualty cc = casualtylist.get(jj);
////														if(s5.casualtyID==cc.casualtyID) {
////															s5.used = true;
////														}
////													}
////													}
////												}
//												if((s5.used == true) && (i5<casualtylist.size()-1)) {
//													continue;
//													}
//												if(s5.used == false) {
//												five.casualtyList.add(s5);
//												s5.used = true;
//												}
//												for(int j5=0;j5<casualtylist.size();++j5){//�ڶ�������
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////														if(t.casualtyList!=null) {
////														for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////															Casualty cc = casualtylist.get(jj);
////															if(ss5.casualtyID==cc.casualtyID) {
////																ss5.used = true;
////															}
////														}
////														}
////													}
//													if((ss5.used == true) && (j5<casualtylist.size()-1)) {
//														continue;
//														}
//													if(ss5.used == false) {
//													five.casualtyList.add(ss5);
//													ss5.used = true;
//													}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							ambulanceList.add(five);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							System.out.println("five size: "+five.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//								fivelist = new ArrayList<Casualty>(five.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							ambulanceList.remove(five);
//							
//							
//							
//							if(five.casualtyList!=null && five.casualtyList.contains(ss5)) {
//								five.casualtyList.remove(ss5);
//								ss5.used = false;
//							}
//						}
//						if(five.casualtyList!=null && five.casualtyList.contains(s5)) {
//							five.casualtyList.remove(s5);
//							s5.used = false;
//						}
//					}
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			five.casualtyList = fivelist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//			bestAmbulanceList.add(five);
//		}
//		if(ambulanceArrived.size()==6) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			Ambulance six=(Ambulance)ambulanceArrived.get(5);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			six.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//					//System.out.println(one.positionFlag);
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//											for(int i5=0;i5<casualtylist.size();++i5){//��һ������ 5��
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////													if(t.casualtyList!=null) {
////													for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////														Casualty cc = casualtylist.get(jj);
////														if(s5.casualtyID==cc.casualtyID) {
////															s5.used = true;
////														}
////													}
////													}
////												}
//												if((s5.used == true) && (i5<casualtylist.size()-1)) {
//													continue;
//													}
//												if(s5.used == false) {
//												five.casualtyList.add(s5);
//												s5.used = true;
//												}
//												for(int j5=0;j5<casualtylist.size();++j5){//�ڶ�������
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////														if(t.casualtyList!=null) {
////														for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////															Casualty cc = casualtylist.get(jj);
////															if(ss5.casualtyID==cc.casualtyID) {
////																ss5.used = true;
////															}
////														}
////														}
////													}
//													if((ss5.used == true) && (j5<casualtylist.size()-1)) {
//														continue;
//														}
//													if(ss5.used == false) {
//													five.casualtyList.add(ss5);
//													ss5.used = true;
//													}
//													for(int i6=0;i6<casualtylist.size();++i6){//��һ������ 6��
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////															if(t.casualtyList!=null) {
////															for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																Casualty cc = casualtylist.get(jj);
////																if(s6.casualtyID==cc.casualtyID) {
////																	s6.used = true;
////																}
////															}
////															}
////														}
//														if((s6.used == true) && (i6<casualtylist.size()-1)) {
//															continue;
//															}
//														if(s6.used == false) {
//														six.casualtyList.add(s6);
//														s6.used = true;
//														}
//														for(int j6=0;j6<casualtylist.size();++j6){//�ڶ�������
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																if(t.casualtyList!=null) {
////																for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																	Casualty cc = casualtylist.get(jj);
////																	if(ss6.casualtyID==cc.casualtyID) {
////																		ss6.used = true;
////																	}
////																}
////																}
////															}
//															if((ss6.used == true) && (j6<casualtylist.size()-1)) {
//																continue;
//																}
//															if(ss6.used == false) {
//															six.casualtyList.add(ss6);
//															ss6.used = true;
//															}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							ambulanceList.add(five);
//							ambulanceList.add(six);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							System.out.println("five size: "+five.casualtyList.size());
//							System.out.println("six size: "+six.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//									System.out.println("2013 "+k);
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//								fivelist = new ArrayList<Casualty>(five.casualtyList);
//								sixlist = new ArrayList<Casualty>(six.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							ambulanceList.remove(five);
//							ambulanceList.remove(six);
//							
//							
//							if(six.casualtyList!=null && six.casualtyList.contains(ss6)) {
//								six.casualtyList.remove(ss6);
//								ss6.used = false;
//							}
//						}
//						if(six.casualtyList!=null && six.casualtyList.contains(s6)) {
//							six.casualtyList.remove(s6);
//							s6.used = false;
//						}
//					}
//							if(five.casualtyList!=null && five.casualtyList.contains(ss5)) {
//								five.casualtyList.remove(ss5);
//								ss5.used = false;
//							}
//						}
//						if(five.casualtyList!=null && five.casualtyList.contains(s5)) {
//							five.casualtyList.remove(s5);
//							s5.used = false;
//						}
//					}
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			five.casualtyList = fivelist;
//			six.casualtyList = sixlist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//			bestAmbulanceList.add(five);
//			bestAmbulanceList.add(six);
//		}
//		if(ambulanceArrived.size()==7) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			Ambulance six=(Ambulance)ambulanceArrived.get(5);
//			Ambulance seven=(Ambulance)ambulanceArrived.get(6);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			six.casualtyList = new ArrayList<Casualty>();
//			seven.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//											for(int i5=0;i5<casualtylist.size();++i5){//��һ������ 5��
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////													if(t.casualtyList!=null) {
////													for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////														Casualty cc = casualtylist.get(jj);
////														if(s5.casualtyID==cc.casualtyID) {
////															s5.used = true;
////														}
////													}
////													}
////												}
//												if((s5.used == true) && (i5<casualtylist.size()-1)) {
//													continue;
//													}
//												if(s5.used == false) {
//												five.casualtyList.add(s5);
//												s5.used = true;
//												}
//												for(int j5=0;j5<casualtylist.size();++j5){//�ڶ�������
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////														if(t.casualtyList!=null) {
////														for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////															Casualty cc = casualtylist.get(jj);
////															if(ss5.casualtyID==cc.casualtyID) {
////																ss5.used = true;
////															}
////														}
////														}
////													}
//													if((ss5.used == true) && (j5<casualtylist.size()-1)) {
//														continue;
//														}
//													if(ss5.used == false) {
//													five.casualtyList.add(ss5);
//													ss5.used = true;
//													}
//													for(int i6=0;i6<casualtylist.size();++i6){//��һ������ 6��
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////															if(t.casualtyList!=null) {
////															for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																Casualty cc = casualtylist.get(jj);
////																if(s6.casualtyID==cc.casualtyID) {
////																	s6.used = true;
////																}
////															}
////															}
////														}
//														if((s6.used == true) && (i6<casualtylist.size()-1)) {
//															continue;
//															}
//														if(s6.used == false) {
//														six.casualtyList.add(s6);
//														s6.used = true;
//														}
//														for(int j6=0;j6<casualtylist.size();++j6){//�ڶ�������
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																if(t.casualtyList!=null) {
////																for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																	Casualty cc = casualtylist.get(jj);
////																	if(ss6.casualtyID==cc.casualtyID) {
////																		ss6.used = true;
////																	}
////																}
////																}
////															}
//															if((ss6.used == true) && (j6<casualtylist.size()-1)) {
//																continue;
//																}
//															if(ss6.used == false) {
//															six.casualtyList.add(ss6);
//															ss6.used = true;
//															}
//															for(int i7=0;i7<casualtylist.size();++i7){//��һ������ 7��
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																	if(t.casualtyList!=null) {
////																	for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																		Casualty cc = casualtylist.get(jj);
////																		if(s7.casualtyID==cc.casualtyID) {
////																			s7.used = true;
////																		}
////																	}
////																	}
////																}
//																if((s7.used == true) && (i7<casualtylist.size()-1)) {
//																	continue;
//																	}
//																if(s7.used == false) {
//																seven.casualtyList.add(s7);
//																s7.used = true;
//																}
//																for(int j7=0;j7<casualtylist.size();++j7){//�ڶ�������
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																		if(t.casualtyList!=null) {
////																		for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																			Casualty cc = casualtylist.get(jj);
////																			if(ss7.casualtyID==cc.casualtyID) {
////																				ss7.used = true;
////																			}
////																		}
////																		}
////																	}
//																	if((ss7.used == true) && (j7<casualtylist.size()-1)) {
//																		continue;
//																		}
//																	if(ss7.used == false) {
//																	seven.casualtyList.add(ss7);
//																	ss7.used = true;
//																	}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							ambulanceList.add(five);
//							ambulanceList.add(six);
//							ambulanceList.add(seven);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							System.out.println("five size: "+five.casualtyList.size());
//							System.out.println("six size: "+six.casualtyList.size());
//							System.out.println("seven size: "+seven.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//									System.out.println("2013 "+k);
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//								fivelist = new ArrayList<Casualty>(five.casualtyList);
//								sixlist = new ArrayList<Casualty>(six.casualtyList);
//								sevenlist = new ArrayList<Casualty>(seven.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							ambulanceList.remove(five);
//							ambulanceList.remove(six);
//							ambulanceList.remove(seven);
//							
//							if(seven.casualtyList!=null && seven.casualtyList.contains(ss7)) {
//								seven.casualtyList.remove(ss7);
//								ss7.used = false;
//							}
//						}
//						if(seven.casualtyList!=null && seven.casualtyList.contains(s7)) {
//							seven.casualtyList.remove(s7);
//							s7.used = false;
//						}
//					}
//							if(six.casualtyList!=null && six.casualtyList.contains(ss6)) {
//								six.casualtyList.remove(ss6);
//								ss6.used = false;
//							}
//						}
//						if(six.casualtyList!=null && six.casualtyList.contains(s6)) {
//							six.casualtyList.remove(s6);
//							s6.used = false;
//						}
//					}
//							if(five.casualtyList!=null && five.casualtyList.contains(ss5)) {
//								five.casualtyList.remove(ss5);
//								ss5.used = false;
//							}
//						}
//						if(five.casualtyList!=null && five.casualtyList.contains(s5)) {
//							five.casualtyList.remove(s5);
//							s5.used = false;
//						}
//					}
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			five.casualtyList = fivelist;
//			six.casualtyList = sixlist;
//			seven.casualtyList = sevenlist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//			bestAmbulanceList.add(five);
//			bestAmbulanceList.add(six);
//			bestAmbulanceList.add(seven);
//		}
//		if(ambulanceArrived.size()==9) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			Ambulance six=(Ambulance)ambulanceArrived.get(5);
//			Ambulance seven=(Ambulance)ambulanceArrived.get(6);
//			Ambulance eight=(Ambulance)ambulanceArrived.get(7);
//			Ambulance nine=(Ambulance)ambulanceArrived.get(8);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			six.casualtyList = new ArrayList<Casualty>();
//			seven.casualtyList = new ArrayList<Casualty>();
//			eight.casualtyList = new ArrayList<Casualty>();
//			nine.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//											for(int i5=0;i5<casualtylist.size();++i5){//��һ������ 5��
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////													if(t.casualtyList!=null) {
////													for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////														Casualty cc = casualtylist.get(jj);
////														if(s5.casualtyID==cc.casualtyID) {
////															s5.used = true;
////														}
////													}
////													}
////												}
//												if((s5.used == true) && (i5<casualtylist.size()-1)) {
//													continue;
//													}
//												if(s5.used == false) {
//												five.casualtyList.add(s5);
//												s5.used = true;
//												}
//												for(int j5=0;j5<casualtylist.size();++j5){//�ڶ�������
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////														if(t.casualtyList!=null) {
////														for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////															Casualty cc = casualtylist.get(jj);
////															if(ss5.casualtyID==cc.casualtyID) {
////																ss5.used = true;
////															}
////														}
////														}
////													}
//													if((ss5.used == true) && (j5<casualtylist.size()-1)) {
//														continue;
//														}
//													if(ss5.used == false) {
//													five.casualtyList.add(ss5);
//													ss5.used = true;
//													}
//													for(int i6=0;i6<casualtylist.size();++i6){//��һ������ 6��
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////															if(t.casualtyList!=null) {
////															for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																Casualty cc = casualtylist.get(jj);
////																if(s6.casualtyID==cc.casualtyID) {
////																	s6.used = true;
////																}
////															}
////															}
////														}
//														if((s6.used == true) && (i6<casualtylist.size()-1)) {
//															continue;
//															}
//														if(s6.used == false) {
//														six.casualtyList.add(s6);
//														s6.used = true;
//														}
//														for(int j6=0;j6<casualtylist.size();++j6){//�ڶ�������
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																if(t.casualtyList!=null) {
////																for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																	Casualty cc = casualtylist.get(jj);
////																	if(ss6.casualtyID==cc.casualtyID) {
////																		ss6.used = true;
////																	}
////																}
////																}
////															}
//															if((ss6.used == true) && (j6<casualtylist.size()-1)) {
//																continue;
//																}
//															if(ss6.used == false) {
//															six.casualtyList.add(ss6);
//															ss6.used = true;
//															}
//															for(int i7=0;i7<casualtylist.size();++i7){//��һ������ 7��
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																	if(t.casualtyList!=null) {
////																	for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																		Casualty cc = casualtylist.get(jj);
////																		if(s7.casualtyID==cc.casualtyID) {
////																			s7.used = true;
////																		}
////																	}
////																	}
////																}
//																if((s7.used == true) && (i7<casualtylist.size()-1)) {
//																	continue;
//																	}
//																if(s7.used == false) {
//																seven.casualtyList.add(s7);
//																s7.used = true;
//																}
//																for(int j7=0;j7<casualtylist.size();++j7){//�ڶ�������
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																		if(t.casualtyList!=null) {
////																		for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																			Casualty cc = casualtylist.get(jj);
////																			if(ss7.casualtyID==cc.casualtyID) {
////																				ss7.used = true;
////																			}
////																		}
////																		}
////																	}
//																	if((ss7.used == true) && (j7<casualtylist.size()-1)) {
//																		continue;
//																		}
//																	if(ss7.used == false) {
//																	seven.casualtyList.add(ss7);
//																	ss7.used = true;
//																	}
//																	for(int i8=0;i8<casualtylist.size();++i8){//��һ������ 8��
//																		Casualty s8 = casualtylist.get(i8);
////																		s8.used = false;
////																		for(int ii=0;ii<ambulanceArrived.size();++ii){
////																			Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																			if(t.casualtyList!=null) {
////																			for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																				Casualty cc = casualtylist.get(jj);
////																				if(s8.casualtyID==cc.casualtyID) {
////																					s8.used = true;
////																				}
////																			}
////																			}
////																		}
//																		if((s8.used == true) && (i8<casualtylist.size()-1)) {
//																			continue;
//																			}
//																		if(s8.used == false) {
//																		eight.casualtyList.add(s8);
//																		s8.used = true;
//																		}
//																		for(int j8=0;j8<casualtylist.size();++j8){//�ڶ�������
//																			Casualty ss8 = casualtylist.get(j8);
////																			ss8.used = false;
////																			for(int ii=0;ii<ambulanceArrived.size();++ii){
////																				Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																				if(t.casualtyList!=null) {
////																				for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																					Casualty cc = casualtylist.get(jj);
////																					if(ss8.casualtyID==cc.casualtyID) {
////																						ss8.used = true;
////																					}
////																				}
////																				}
////																			}
//																			if((ss8.used == true) && (j8<casualtylist.size()-1)) {
//																				continue;
//																				}
//																			if(ss8.used == false) {
//																			eight.casualtyList.add(ss8);
//																			ss8.used = true;
//																			}
//																			for(int i9=0;i9<casualtylist.size();++i9){//��һ������ 9��
//																				Casualty s9 = casualtylist.get(i9);
////																				s9.used = false;
////																				for(int ii=0;ii<ambulanceArrived.size();++ii){
////																					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																					if(t.casualtyList!=null) {
////																					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																						Casualty cc = casualtylist.get(jj);
////																						if(s9.casualtyID==cc.casualtyID) {
////																							s9.used = true;
////																						}
////																					}
////																					}
////																				}
//																				if((s9.used == true) && (i9<casualtylist.size()-1)) {
//																					continue;
//																					}
//																				if(s9.used == false) {
//																				nine.casualtyList.add(s9);
//																				s9.used = true;
//																				}
//																				for(int j9=0;j9<casualtylist.size();++j9){//�ڶ�������
//																					Casualty ss9 = casualtylist.get(j9);
////																					ss9.used = false;
////																					for(int ii=0;ii<ambulanceArrived.size();++ii){
////																						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																						if(t.casualtyList!=null) {
////																						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																							Casualty cc = casualtylist.get(jj);
////																							if(ss9.casualtyID==cc.casualtyID) {
////																								ss9.used = true;
////																							}
////																						}
////																						}
////																					}
//																					if((ss9.used == true) && (j9<casualtylist.size()-1)) {
//																						continue;
//																						}
//																					if(ss9.used == false) {
//																					nine.casualtyList.add(ss9);
//																					ss9.used = true;
//																					}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							ambulanceList.add(five);
//							ambulanceList.add(six);
//							ambulanceList.add(seven);
//							ambulanceList.add(eight);
//							ambulanceList.add(nine);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							System.out.println("five size: "+five.casualtyList.size());
//							System.out.println("six size: "+six.casualtyList.size());
//							System.out.println("seven size: "+seven.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//								fivelist = new ArrayList<Casualty>(five.casualtyList);
//								sixlist = new ArrayList<Casualty>(six.casualtyList);
//								sevenlist = new ArrayList<Casualty>(seven.casualtyList);
//								eightlist = new ArrayList<Casualty>(eight.casualtyList);
//								ninelist = new ArrayList<Casualty>(nine.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							ambulanceList.remove(five);
//							ambulanceList.remove(six);
//							ambulanceList.remove(seven);
//							ambulanceList.remove(eight);
//							ambulanceList.remove(nine);
//							
//							if(nine.casualtyList!=null && nine.casualtyList.contains(ss9)) {
//								nine.casualtyList.remove(ss9);
//								ss9.used = false;
//							}
//						}
//						if(nine.casualtyList!=null && nine.casualtyList.contains(s9)) {
//							nine.casualtyList.remove(s9);
//							s9.used = false;
//						}
//					}
//							if(eight.casualtyList!=null && eight.casualtyList.contains(ss8)) {
//								eight.casualtyList.remove(ss8);
//								ss8.used = false;
//							}
//						}
//						if(eight.casualtyList!=null && eight.casualtyList.contains(s8)) {
//							eight.casualtyList.remove(s8);
//							s8.used = false;
//						}
//					}
//							if(seven.casualtyList!=null && seven.casualtyList.contains(ss7)) {
//								seven.casualtyList.remove(ss7);
//								ss7.used = false;
//							}
//						}
//						if(seven.casualtyList!=null && seven.casualtyList.contains(s7)) {
//							seven.casualtyList.remove(s7);
//							s7.used = false;
//						}
//					}
//							if(six.casualtyList!=null && six.casualtyList.contains(ss6)) {
//								six.casualtyList.remove(ss6);
//								ss6.used = false;
//							}
//						}
//						if(six.casualtyList!=null && six.casualtyList.contains(s6)) {
//							six.casualtyList.remove(s6);
//							s6.used = false;
//						}
//					}
//							if(five.casualtyList!=null && five.casualtyList.contains(ss5)) {
//								five.casualtyList.remove(ss5);
//								ss5.used = false;
//							}
//						}
//						if(five.casualtyList!=null && five.casualtyList.contains(s5)) {
//							five.casualtyList.remove(s5);
//							s5.used = false;
//						}
//					}
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			five.casualtyList = fivelist;
//			six.casualtyList = sixlist;
//			seven.casualtyList = sevenlist;
//			eight.casualtyList = eightlist;
//			nine.casualtyList = ninelist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//			bestAmbulanceList.add(five);
//			bestAmbulanceList.add(six);
//			bestAmbulanceList.add(seven);
//			bestAmbulanceList.add(eight);
//			bestAmbulanceList.add(nine);
//		}
//		if(ambulanceArrived.size()==8) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			Ambulance six=(Ambulance)ambulanceArrived.get(5);
//			Ambulance seven=(Ambulance)ambulanceArrived.get(6);
//			Ambulance eight=(Ambulance)ambulanceArrived.get(7);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			six.casualtyList = new ArrayList<Casualty>();
//			seven.casualtyList = new ArrayList<Casualty>();
//			eight.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//��һ������ 1��
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//��obj����תΪAmbulance����
////					if(t.casualtyList!=null) {
////					for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////						Casualty cc = casualtylist.get(jj);
////						if(s.casualtyID==cc.casualtyID) {
////							s.used = true;
////						}
////					}
////					}
////				}
//				if((s.used == true) && (i<casualtylist.size()-1)) {
//				continue;
//				}
//				if(s.used == false) {
//				one.casualtyList.add(s);
//				s.used = true;
//				}
//				for(int j=0;j<casualtylist.size();++j){//�ڶ�������
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////						if(t.casualtyList!=null) {
////						for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////							Casualty cc = casualtylist.get(jj);
////							if(ss.casualtyID==cc.casualtyID) {
////								ss.used = true;
////							}
////						}
////						}
////					}
//					if((ss.used == true) && (j<casualtylist.size()-1)) {
//						continue;
//						}
//					if(ss.used == false) {
//					one.casualtyList.add(ss);
//					ss.used = true;
//					}
//					for(int i2=0;i2<casualtylist.size();++i2){//��һ������ 2��
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////							if(t.casualtyList!=null) {
////							for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////								Casualty cc = casualtylist.get(jj);
////								if(s2.casualtyID==cc.casualtyID) {
////									s2.used = true;
////								}
////							}
////							}
////						}
//						if((s2.used == true) && (i2<casualtylist.size()-1)) {
//							continue;
//							}
//						if(s2.used == false) {
//						two.casualtyList.add(s2);
//						s2.used = true;
//						}
//						for(int j2=0;j2<casualtylist.size();++j2){//�ڶ�������
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////								if(t.casualtyList!=null) {
////								for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////									Casualty cc = casualtylist.get(jj);
////									if(ss2.casualtyID==cc.casualtyID) {
////										ss2.used = true;
////									}
////								}
////								}
////							}
//							if((ss2.used == true) && (j2<casualtylist.size()-1)) {
//								continue;
//								}
//							if(ss2.used == false) {
//							two.casualtyList.add(ss2);
//							ss2.used = true;
//							}
//							for(int i3=0;i3<casualtylist.size();++i3){//��һ������ 3��
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////									if(t.casualtyList!=null) {
////									for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////										Casualty cc = casualtylist.get(jj);
////										if(s3.casualtyID==cc.casualtyID) {
////											s3.used = true;
////										}
////									}
////									}
////								}
//								if((s3.used == true) && (i3<casualtylist.size()-1)) {
//									continue;
//									}
//								if(s3.used == false) {
//								three.casualtyList.add(s3);
//								s3.used = true;
//								}
//								for(int j3=0;j3<casualtylist.size();++j3){//�ڶ�������
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////										if(t.casualtyList!=null) {
////										for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////											Casualty cc = casualtylist.get(jj);
////											if(ss3.casualtyID==cc.casualtyID) {
////												ss3.used = true;
////											}
////										}
////										}
////									}
//									if((ss3.used == true) && (j3<casualtylist.size()-1)) {
//										continue;
//										}
//									if(ss3.used == false) {
//									three.casualtyList.add(ss3);
//									ss3.used = true;
//									}
//									for(int i4=0;i4<casualtylist.size();++i4){//��һ������ 4��
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////											if(t.casualtyList!=null) {
////											for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////												Casualty cc = casualtylist.get(jj);
////												if(s4.casualtyID==cc.casualtyID) {
////													s4.used = true;
////												}
////											}
////											}
////										}
//										if((s4.used == true) && (i4<casualtylist.size()-1)) {
//											continue;
//											}
//										if(s4.used == false) {
//										four.casualtyList.add(s4);
//										s4.used = true;
//										}
//										for(int j4=0;j4<casualtylist.size();++j4){//�ڶ�������
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////												if(t.casualtyList!=null) {
////												for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////													Casualty cc = casualtylist.get(jj);
////													if(ss4.casualtyID==cc.casualtyID) {
////														ss4.used = true;
////													}
////												}
////												}
////											}
//											if((ss4.used == true) && (j4<casualtylist.size()-1)) {
//												continue;
//												}
//											if(ss4.used == false) {
//											four.casualtyList.add(ss4);
//											ss4.used = true;
//											}
//											for(int i5=0;i5<casualtylist.size();++i5){//��һ������ 5��
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////													if(t.casualtyList!=null) {
////													for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////														Casualty cc = casualtylist.get(jj);
////														if(s5.casualtyID==cc.casualtyID) {
////															s5.used = true;
////														}
////													}
////													}
////												}
//												if((s5.used == true) && (i5<casualtylist.size()-1)) {
//													continue;
//													}
//												if(s5.used == false) {
//												five.casualtyList.add(s5);
//												s5.used = true;
//												}
//												for(int j5=0;j5<casualtylist.size();++j5){//�ڶ�������
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////														if(t.casualtyList!=null) {
////														for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////															Casualty cc = casualtylist.get(jj);
////															if(ss5.casualtyID==cc.casualtyID) {
////																ss5.used = true;
////															}
////														}
////														}
////													}
//													if((ss5.used == true) && (j5<casualtylist.size()-1)) {
//														continue;
//														}
//													if(ss5.used == false) {
//													five.casualtyList.add(ss5);
//													ss5.used = true;
//													}
//													for(int i6=0;i6<casualtylist.size();++i6){//��һ������ 6��
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////															if(t.casualtyList!=null) {
////															for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																Casualty cc = casualtylist.get(jj);
////																if(s6.casualtyID==cc.casualtyID) {
////																	s6.used = true;
////																}
////															}
////															}
////														}
//														if((s6.used == true) && (i6<casualtylist.size()-1)) {
//															continue;
//															}
//														if(s6.used == false) {
//														six.casualtyList.add(s6);
//														s6.used = true;
//														}
//														for(int j6=0;j6<casualtylist.size();++j6){//�ڶ�������
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																if(t.casualtyList!=null) {
////																for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																	Casualty cc = casualtylist.get(jj);
////																	if(ss6.casualtyID==cc.casualtyID) {
////																		ss6.used = true;
////																	}
////																}
////																}
////															}
//															if((ss6.used == true) && (j6<casualtylist.size()-1)) {
//																continue;
//																}
//															if(ss6.used == false) {
//															six.casualtyList.add(ss6);
//															ss6.used = true;
//															}
//															for(int i7=0;i7<casualtylist.size();++i7){//��һ������ 7��
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																	if(t.casualtyList!=null) {
////																	for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																		Casualty cc = casualtylist.get(jj);
////																		if(s7.casualtyID==cc.casualtyID) {
////																			s7.used = true;
////																		}
////																	}
////																	}
////																}
//																if((s7.used == true) && (i7<casualtylist.size()-1)) {
//																	continue;
//																	}
//																if(s7.used == false) {
//																seven.casualtyList.add(s7);
//																s7.used = true;
//																}
//																for(int j7=0;j7<casualtylist.size();++j7){//�ڶ�������
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																		if(t.casualtyList!=null) {
////																		for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																			Casualty cc = casualtylist.get(jj);
////																			if(ss7.casualtyID==cc.casualtyID) {
////																				ss7.used = true;
////																			}
////																		}
////																		}
////																	}
//																	if((ss7.used == true) && (j7<casualtylist.size()-1)) {
//																		continue;
//																		}
//																	if(ss7.used == false) {
//																	seven.casualtyList.add(ss7);
//																	ss7.used = true;
//																	}
//																	for(int i8=0;i8<casualtylist.size();++i8){//��һ������ 8��
//																		Casualty s8 = casualtylist.get(i8);
////																		s8.used = false;
////																		for(int ii=0;ii<ambulanceArrived.size();++ii){
////																			Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																			if(t.casualtyList!=null) {
////																			for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																				Casualty cc = casualtylist.get(jj);
////																				if(s8.casualtyID==cc.casualtyID) {
////																					s8.used = true;
////																				}
////																			}
////																			}
////																		}
//																		if((s8.used == true) && (i8<casualtylist.size()-1)) {
//																			continue;
//																			}
//																		if(s8.used == false) {
//																		eight.casualtyList.add(s8);
//																		s8.used = true;
//																		}
//																		for(int j8=0;j8<casualtylist.size();++j8){//�ڶ�������
//																			Casualty ss8 = casualtylist.get(j8);
////																			ss8.used = false;
////																			for(int ii=0;ii<ambulanceArrived.size();++ii){
////																				Ambulance t=(Ambulance)ambulanceArrived.get(ii);//��obj����תΪAmbulance����
////																				if(t.casualtyList!=null) {
////																				for(int jj=0; jj<t.casualtyList.size(); ++jj) {
////																					Casualty cc = casualtylist.get(jj);
////																					if(ss8.casualtyID==cc.casualtyID) {
////																						ss8.used = true;
////																					}
////																				}
////																				}
////																			}
//																			if((ss8.used == true) && (j8<casualtylist.size()-1)) {
//																				continue;
//																				}
//																			if(ss8.used == false) {
//																			eight.casualtyList.add(ss8);
//																			ss8.used = true;
//																			}
//							
//							
//							
//							
//							int sumRPM  = 0;            //RPM֮��
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							ambulanceList.add(five);
//							ambulanceList.add(six);
//							ambulanceList.add(seven);
//							ambulanceList.add(eight);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							System.out.println("five size: "+five.casualtyList.size());
//							System.out.println("six size: "+six.casualtyList.size());
//							System.out.println("seven size: "+seven.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //��ʼ�����ϵ��ж�
//								Ambulance am=(Ambulance)ambulanceList.get(k);
//								if(am.casualtyList!=null && am.casualtyList.size()>0){
//								am.target_hospital = chooseHospitalBasedOnRPM(am.casualtyList).hos;
//								sumRPM+=chooseHospitalBasedOnRPM(am.casualtyList).sum;
//								}						
//							}
//							if(sumRPM>bestRPM) {
//								System.out.println("bestrpm:"+sumRPM);
//								bestRPM = sumRPM;
//								onelist = new ArrayList<Casualty>(one.casualtyList);
//								twolist = new ArrayList<Casualty>(two.casualtyList);
//								threelist = new ArrayList<Casualty>(three.casualtyList);
//								fourlist = new ArrayList<Casualty>(four.casualtyList);
//								fivelist = new ArrayList<Casualty>(five.casualtyList);
//								sixlist = new ArrayList<Casualty>(six.casualtyList);
//								sevenlist = new ArrayList<Casualty>(seven.casualtyList);
//								eightlist = new ArrayList<Casualty>(eight.casualtyList);
//							}
//							ambulanceList.remove(one);
//							ambulanceList.remove(two);
//							ambulanceList.remove(three);
//							ambulanceList.remove(four);
//							ambulanceList.remove(five);
//							ambulanceList.remove(six);
//							ambulanceList.remove(seven);
//							ambulanceList.remove(eight);
//							
//							
//							if(eight.casualtyList!=null && eight.casualtyList.contains(ss8)) {
//								eight.casualtyList.remove(ss8);
//								ss8.used = false;
//							}
//						}
//						if(eight.casualtyList!=null && eight.casualtyList.contains(s8)) {
//							eight.casualtyList.remove(s8);
//							s8.used = false;
//						}
//					}
//							if(seven.casualtyList!=null && seven.casualtyList.contains(ss7)) {
//								seven.casualtyList.remove(ss7);
//								ss7.used = false;
//							}
//						}
//						if(seven.casualtyList!=null && seven.casualtyList.contains(s7)) {
//							seven.casualtyList.remove(s7);
//							s7.used = false;
//						}
//					}
//							if(six.casualtyList!=null && six.casualtyList.contains(ss6)) {
//								six.casualtyList.remove(ss6);
//								ss6.used = false;
//							}
//						}
//						if(six.casualtyList!=null && six.casualtyList.contains(s6)) {
//							six.casualtyList.remove(s6);
//							s6.used = false;
//						}
//					}
//							if(five.casualtyList!=null && five.casualtyList.contains(ss5)) {
//								five.casualtyList.remove(ss5);
//								ss5.used = false;
//							}
//						}
//						if(five.casualtyList!=null && five.casualtyList.contains(s5)) {
//							five.casualtyList.remove(s5);
//							s5.used = false;
//						}
//					}
//							if(four.casualtyList!=null && four.casualtyList.contains(ss4)) {
//								four.casualtyList.remove(ss4);
//								ss4.used = false;
//							}
//						}
//						if(four.casualtyList!=null && four.casualtyList.contains(s4)) {
//							four.casualtyList.remove(s4);
//							s4.used = false;
//						}
//					}
//							if(three.casualtyList!=null && three.casualtyList.contains(ss3)) {
//								three.casualtyList.remove(ss3);
//								ss3.used = false;
//							}
//						}
//						if(three.casualtyList!=null && three.casualtyList.contains(s3)) {
//							three.casualtyList.remove(s3);
//							s3.used = false;
//						}
//					}
//							if(two.casualtyList!=null && two.casualtyList.contains(ss2)) {
//								two.casualtyList.remove(ss2);
//								ss2.used = false;
//							}
//						}
//						if(two.casualtyList!=null && two.casualtyList.contains(s2)) {
//							two.casualtyList.remove(s2);
//							s2.used = false;
//						}
//					}
//					if(one.casualtyList!=null && one.casualtyList.contains(ss)) {
//						one.casualtyList.remove(ss);
//						ss.used = false;
//					}
//				}
//				if(one.casualtyList!=null && one.casualtyList.contains(s)) {
//					one.casualtyList.remove(s);
//					s.used = false;
//				}
//			}
//			one.casualtyList = onelist;
//			two.casualtyList = twolist;
//			three.casualtyList = threelist;
//			four.casualtyList = fourlist;
//			five.casualtyList = fivelist;
//			six.casualtyList = sixlist;
//			seven.casualtyList = sevenlist;
//			eight.casualtyList = eightlist;
//			bestAmbulanceList.add(one);
//			bestAmbulanceList.add(two);
//			bestAmbulanceList.add(three);
//			bestAmbulanceList.add(four);
//			bestAmbulanceList.add(five);
//			bestAmbulanceList.add(six);
//			bestAmbulanceList.add(seven);
//			bestAmbulanceList.add(eight);
//		}
//
//		
//		}
		else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_6) {
			 
			if(ambulanceArrived.size()==1) {
				Ambulance one=(Ambulance)ambulanceArrived.get(0);
				one.casualtyList=casualtylist;
				one.target_hospital = chooseHospitalBasedOnRPM(one.casualtyList).hos;
				bestAmbulanceList.add(one);
			}
			else {
				System.out.println("����������");
				int[][] matlabR1 = giveRPMTable(casualtylist);
				int matlabn1 = ambulanceArrived.size();
				int[][] matlabR2 = giveflyRPMTable(casualtylist);
				int matlabn2 = 0;
				Object[] result = null; // ���ڱ��������
				MWNumericArray n1 = null;
				MWNumericArray n2 = null;
				MWNumericArray R1 = null;
				MWNumericArray R2 = null;
//				adddd adddd = null;
				mci2 mci2 = null;
				try {
					mci2 = new mci2();
					n1 = new MWNumericArray(matlabn1, MWClassID.DOUBLE);
					n2 = new MWNumericArray(matlabn2, MWClassID.DOUBLE);
					R1 = new MWNumericArray(matlabR1, MWClassID.DOUBLE);
					R2 = new MWNumericArray(matlabR2, MWClassID.DOUBLE);
					result = mci2.testing1(1,R1,R2,n1,n2);
					MWNumericArray output=(MWNumericArray) result[0];
					System.out.println("41320 "+output);
				} 
				catch (MWException e) {
					 System.out.println("Exception: " + e.toString());
				}
				finally {
			         // �ͷű�����Դ
					MWArray.disposeArray(n1);
					MWArray.disposeArray(n2);
					MWArray.disposeArray(R1);
					MWArray.disposeArray(R2);
					MWArray.disposeArray(result);
					if(mci2!=null) {
			         mci2.dispose();
					}

			      }
//				System.out.println("4132 "+matlabR1);
				System.out.println("4132 "+matlabR1[0][0]+"4132 "+matlabR1[0][1]+"4132 "+matlabR1[0][2]);// 
				System.out.println("4132 "+matlabR1[1][0]+"4132 "+matlabR1[1][1]+"4132 "+matlabR1[1][2]);//
				System.out.println("4132 "+matlabR1[2][0]+"4132 "+matlabR1[2][1]+"4132 "+matlabR1[2][2]);//
				System.out.println("4132 "+matlabR1[3][0]+"4132 "+matlabR1[3][1]+"4132 "+matlabR1[3][2]);//
				System.out.println("4133 "+matlabn1);// 
			}
	}
		
		return bestAmbulanceList;
	}
	private int newExpectRPM(Casualty casualty, Hospital hospital){     //ʹ�õ�ǰ��RPM������Ԥ�ڶ����ǳ�ʼ zyh
		//ѡȡcasualty����
	//	List<Object> localCasualty=new ArrayList<Object>(); //�����ڲ�ʹ�õ���Ա�б�
				
		int expectRPM=0;// ��ԱԤ��RPM
		double currentTime = MCIContextBuilder.currentTime;
		//double[][] allHospitalAvgWaitTime=getAvgWaitTime();//��ȡ��ǰÿ��ҽԺ�У�ÿ�����ҵ�ƽ���ȴ�ʱ��
		double[][] allHospitalAvgWaitTime=getExpWaitTime2();//ʹ���µ�Ԥ�ڵȴ�ʱ��    zyh
		//Casualty aCasualty=(Casualty)localCasualty.get(0);//��ȡ ��һ��Ԫ��  ����ֻ��һ��Ԫ�أ���ʾ�������Ա����
		//Hospital aHospital=(Hospital)localHospital.get(0);//��ȡ ��һ��Ԫ��  ֻ��һ��Ԫ�� ��ʾ�����ҽԺ����
		//��ȡ���ֳ���ҽԺʱ�ľ���
		double dis=Math.abs(Constants.MCI_X-hospital.x)+Math.abs(Constants.MCI_Y-hospital.y);  //���� ��Ա��ҽԺ֮��ľ���
		double travelTime=Math.ceil(dis/Constants.AMBULANCE_TRAVEL_SPEED);   //���� ��Ա���͵�ҽԺ��ʱ��
		//����Ԥ��RPM
		//expectRPM=getCurrentRPM(currentTime+travelTime,casualty.InitialRPM);
		expectRPM=getCurrentRPM(currentTime-casualty.triageTime+travelTime,casualty.InitialRPM); //����ʱ��rpm�����ǳ�ʼrpm  ���Եȴ�ʱ����Ǵ�ʱʱ��
//		expectRPM=getCurrentRPM(travelTime,casualty.InitialRPM);
		if(expectRPM<5){//��ʾ��Ա����ҽԺ֮����Ҫ����ICU
			//����expectRPM
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
			
		}else if(expectRPM<9){//��ʾ��Ա����ҽԺ֮����Ҫ����GW
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
			
		}else{//��ʾ��Ա����ҽԺ֮����Ҫ����ED
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//�����ȴ�ʱ��Ϊ ����ʱ��+�ڸ�ҽԺICU�ĵȴ�ʱ��
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);		
		}
		
		return expectRPM;//����
		
		
	}
	//�����Ŷ�����Ԥ�ڵȴ�ʱ��2  zyh //����ֵ��2 4 3
	 private double[][] getExpWaitTime2(){
		 double[][] avgTreatTime = getAvgTreatTime(); //ƽ������ʱ��
		 double[][] expWaitTime = new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];
		 int[][] queNum = new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //��ǰ�Ŷ�����
		 for(int c=0; c<MCIContextBuilder.hospitalList.size(); ++c) {
     		Hospital aHospital = MCIContextBuilder.hospitalList.get(c);
					if(aHospital.waitEDCasualtyList != null) {
					queNum[aHospital.hid][Constants.DEPT_ED] = aHospital.waitEDCasualtyList.size();
					}else {queNum[aHospital.hid][Constants.DEPT_ED] = 0;}
					if(aHospital.waitICUCasualtyList != null) {
					queNum[aHospital.hid][Constants.DEPT_ICU] = aHospital.waitICUCasualtyList.size();
					}else {queNum[aHospital.hid][Constants.DEPT_ICU] = 0;}
					if(aHospital.waitGWCasualtyList != null) {
					queNum[aHospital.hid][Constants.DEPT_GW] = aHospital.waitGWCasualtyList.size();
					}else {queNum[aHospital.hid][Constants.DEPT_GW] = 0;}
				
			}
		 for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				for(int j=0;j<Constants.DEPT_COUNT;++j){
					if(queNum[i][j]==0){//����Ŷ���Ա����Ϊ0
						expWaitTime[i][j]=0;//ֱ�Ӹ�ֵΪ0
					}else{
						expWaitTime[i][j]=avgTreatTime[i][j]*queNum[i][j];
					}
					
				}
			}
		 //ICU�ĵȴ�ʱ�����ED�ĵȴ�ʱ�䣬GWͬ��
		 for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
			 expWaitTime[i][Constants.DEPT_ICU]=Math.max(expWaitTime[i][Constants.DEPT_ED],expWaitTime[i][Constants.DEPT_ICU]);
			 expWaitTime[i][Constants.DEPT_GW]=Math.max(expWaitTime[i][Constants.DEPT_ED],expWaitTime[i][Constants.DEPT_GW]);
			 if (expWaitTime[i][Constants.DEPT_ED] == 0) {
				 expWaitTime[i][Constants.DEPT_ED] = 2;
			 }
			 if (expWaitTime[i][Constants.DEPT_ICU] == 0) {
				 expWaitTime[i][Constants.DEPT_ICU] = 4;
			 }
			 if (expWaitTime[i][Constants.DEPT_GW] == 0) {
				 expWaitTime[i][Constants.DEPT_GW] = 3;
			 }
		 }
		 
		 
		 return expWaitTime;
	 }
	 //��ȡƽ������ʱ��  zyh
	 private double[][] getAvgTreatTime(){
		 double[][] treatTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//��¼ÿ��ҽԺ��ÿ�����ҵ���������ʱ��
			int[][]    treatedCasualty=new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//��¼ÿ��ҽԺ��ÿ�����Ҵ������Ա������
			double[][] avgTreatTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //ƽ������ʱ��
			
			for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
				Casualty aCasualty = MCIContextBuilder.casualtyList.get(a);
					//if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// ͳ�Ƶ�ǰ��Ա��ҽԺ�ĵȴ�ʱ��  ֻͳ�Ƶ�ǰ��Ժ��Ա�ĵȴ�ʱ�䣬�������Ѿ���Ժ����Ժ����������Ա�ĵȴ�ʱ��
					if (aCasualty.arrHospitalTime!=0.0) {// ��Ա����ҽԺʱ�䲻Ϊ0��������Ա�����͵���ĳҽԺ����չ����Ա�ķ�Χ������������ǰ��Ժ����Ա���������Ѿ���Ժ����Ա���Լ���Ժ����������Ա
								
						double treatEDTime = 0.0; // ��ʼֵΪ0.0
						double treatICUTime = 0.0; // ��ʼֵΪ0.0
						double treatGWTime = 0.0; // ��ʼֵΪ0.0
						if (aCasualty.overEDTime != 0.0) {// �����Ա����ED�����enterEDTime
															
							treatEDTime = aCasualty.overEDTime
									- (aCasualty.enterEDTime);// ��Ա����ED��ʱ��
							//������arrHospitalID ��ED �н��ܹ����Ƶ���Ա
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ED]++;
						}
						if (aCasualty.overICUTime != 0.0)
								 {// ��ʾ��Ա����ICU
							// �������ICU��ʱ��
							treatICUTime = aCasualty.overICUTime
									- aCasualty.enterICUTime;
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
						}
						if (aCasualty.overICUTime != 0.0)
								 {// ��ʾ��Ա����GW
							// ���� ����GW��ʱ��
							treatGWTime = aCasualty.overGWTime
									- aCasualty.enterGWTime;
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;

						}
						

						// Ϊ����Ա����ҽԺ����� �ȴ�ʱ��
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_ED] += treatEDTime/Constants.ED_AVAILABLE_COUNT;
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_ICU] += treatICUTime/Constants.ICU_AVAILABLE_COUNT;
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_GW] += treatGWTime/Constants.GW_AVAILABLE_COUNT;
					}
				
			}
			
			//����ÿ��ҽԺ��ÿ�����ҵ�ƽ���ȴ�ʱ��
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				for(int j=0;j<Constants.DEPT_COUNT;++j){
					if(treatedCasualty[i][j]==0){//���������Ա����Ϊ0
						avgTreatTime[i][j]=0;//ֱ�Ӹ�ֵΪ0
					}else{
						avgTreatTime[i][j]=treatTime[i][j]/treatedCasualty[i][j];
					}
					
				}
			}
			return avgTreatTime;
		 
	 }
	// ����Ա���з�������
	private Integer setTriageTag() {
		// ��ѡȡ����casualty����Ȼ��Ϊ����û��tag����Ա������RPM����tag��ÿ��ָ�����Constants.TRIAGE_NUM_AT_ONE_TIME��������Ա��Ч�ʲ���
		List<Casualty> casualtiesIncidentNotTag = new ArrayList<Casualty>();
		for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
			Casualty dd = MCIContextBuilder.casualtyList.get(a);
				if ((dd.casualtyPositionFlag == Constants.POSITION_INCIDENT)
						&& (dd.triageTag == null)) {
					casualtiesIncidentNotTag.add(dd);// ѡȡ�����ֳ���û��tag��casualty����
				}
		}

		if (casualtiesIncidentNotTag.size() > 0) {
			int temp = 0;
			double currentTime = MCIContextBuilder.currentTime;
			do {
				Random r = new Random();
        		
				int index = r.nextInt(casualtiesIncidentNotTag.size());// ��Casualties�����ѡȡһ����Ա
				Object obj = casualtiesIncidentNotTag.get(index);
				Casualty aa = (Casualty) obj; // ������ת��ΪCasualty����
				int casualtyIniRPM = getInitialRPM(aa.RPM,currentTime);  //ʹ�ó�ʼRPM���˷�������Ǵ�ʱ��  zyh
				//int casualtyIniRPM = aa.RPM;
				temp++;
				String tag = null;
				if (casualtyIniRPM == 0 || aa.RPM == 0) { // ��Ա��RPM����0��������Ա�Ѿ�����
					tag = Constants.TAG_BLACK;
				} else if ((casualtyIniRPM > 0) && (casualtyIniRPM < 5)) { // ��ԱRPM��1-4
																			// ֮��,��ʶΪred
					tag = Constants.TAG_RED;
				} else if ((casualtyIniRPM > 4) && (casualtyIniRPM < 9)) { // ��ԱRPM
																			// ��5-8֮�䣬��ʶΪyellow
					tag = Constants.TAG_YELLOW;
				} else {
					tag = Constants.TAG_GREEN;
				}
				aa.triageTag = tag;// ����ǩ����ֵ����casualty����
				aa.triageTime = currentTime;
				System.out.println("Casualty " + aa.casualtyID + " tag "
						+ aa.triageTag.toString());// �����Ա�Ѿ�������Ҫ���м�¼��
				casualtiesIncidentNotTag.remove(aa);
			} while (temp < Constants.TRIAGE_NUM_AT_ONE_TIME && casualtiesIncidentNotTag.size()>0);

		}
		return casualtiesIncidentNotTag.size();

	}
	private int getInitialRPM(int cRPM, double t) {        //�������ڵ�rpm���ҳ�ʼrpm
		int iniRPM = 0;
		for(int aa=0;aa<Constants.rpmDeterationRecord.length-1;++aa){
			if((int)t>=Constants.rpmDeterationRecord[aa][0]){
				for(int bb=1;bb<Constants.rpmDeterationRecord[aa].length;++bb){
					if(cRPM==Constants.rpmDeterationRecord[aa][bb]){
						iniRPM=Constants.rpmDeterationRecord[0][bb];
					}
				}
				
			}
		}
		return iniRPM;
	}
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
	//��ȡƽ���ȴ�ʱ�����飬����ÿ��ҽԺ��ÿ�����ҵ�ƽ���ȴ�ʱ��
	 private double[][] getAvgWaitTime(){
				double[][] waitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//��¼ÿ��ҽԺ��ÿ�����ҵ�����ȴ�ʱ��
				int[][]    treatedCasualty=new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//��¼ÿ��ҽԺ��ÿ�����Ҵ������Ա������
				double[][] avgWaitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //ƽ���ȴ�ʱ��
				// ������Ա��ҽԺ�ĵȴ�ʱ��
				
						for(int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {
			        		Casualty aCasualty = MCIContextBuilder.casualtyList.get(a);
						
						//if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// ͳ�Ƶ�ǰ��Ա��ҽԺ�ĵȴ�ʱ��  ֻͳ�Ƶ�ǰ��Ժ��Ա�ĵȴ�ʱ�䣬�������Ѿ���Ժ����Ժ����������Ա�ĵȴ�ʱ��
						if (aCasualty.arrHospitalTime!=0.0) {// ��Ա����ҽԺʱ�䲻Ϊ0��������Ա�����͵���ĳҽԺ����չ����Ա�ķ�Χ������������ǰ��Ժ����Ա���������Ѿ���Ժ����Ա���Լ���Ժ����������Ա
									
							double waitEDTime = 0.0; // ��ʼֵΪ0.0
							double waitICUTime = 0.0; // ��ʼֵΪ0.0
							double waitGWTime = 0.0; // ��ʼֵΪ0.0
							if (aCasualty.enterEDTime != 0.0) {// �����Ա����ED�����enterEDTime
																// Ϊ0.0
																// ��ʾ��Ա�����˵ȴ��б���ʱ�ݲ�����ED�ȴ�ʱ��
								waitEDTime = aCasualty.enterEDTime
										- (aCasualty.arrHospitalTime);// ��Ա�ȴ�����ED��ʱ��
								//������arrHospitalID ��ED �н��ܹ����Ƶ���Ա
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ED]++;
							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime != 0.0)
									&& ((aCasualty.enterGWTime == 0.0))) {// ��ʾ��Ա����ICU����û�н���GW
								// ����ȴ�����ICU��ʱ��
								waitICUTime = aCasualty.enterICUTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime == 0.0)
									&& ((aCasualty.enterGWTime != 0.0))) {// ��ʾ��Ա����GW��û�н���ICU
								// ���� ��ED�ȴ�����GW��ʱ��
								waitGWTime = aCasualty.enterGWTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;

							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime != 0.0)
									&& ((aCasualty.enterGWTime != 0.0))) {// ��ʾ��Ա�Ƚ���ICU,Ȼ���ٽ���GW
								// ���� ��ICU�ȴ�����GW��ʱ��
								waitGWTime = aCasualty.enterGWTime
										- aCasualty.leaveICUTime;
							
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;
								
								waitICUTime = aCasualty.enterICUTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
							}

							// Ϊ����Ա����ҽԺ����� �ȴ�ʱ��
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_ED] += waitEDTime;
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_ICU] += waitICUTime;
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_GW] += waitGWTime;
						}
					
				}
				
				//����ÿ��ҽԺ��ÿ�����ҵ�ƽ���ȴ�ʱ��
				for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
					for(int j=0;j<Constants.DEPT_COUNT;++j){
						if(treatedCasualty[i][j]==0){//���������Ա����Ϊ0
							avgWaitTime[i][j]=0;//ֱ�Ӹ�ֵΪ0
						}else{
							avgWaitTime[i][j]=waitTime[i][j]/treatedCasualty[i][j];
						}
						
					}
				}
				return avgWaitTime;//����ƽ���ȴ�ʱ���б�
				
				
			}
		class Result {      //������ĺ������ݽ����
		    Hospital hos;
		    int sum;
		}
		private int[][] giveRPMTable(List<Casualty> CasualtyList){
					int[][] expectRPM=new int[CasualtyList.size()][Constants.HOSPITAL_COUNT];
//					System.out.println("4152 "+expectRPM.length);//
					for(int i=0;i<CasualtyList.size();++i){
						for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
							Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//��ҽԺ������ѡȡһ�� ҽԺ����
							expectRPM[i][oneHospital.hid]=newExpectRPM(CasualtyList.get(i),oneHospital);
							}
						
					}
//					System.out.println("4160 "+expectRPM.length);//
			return expectRPM;
		}
		private int[][] giveflyRPMTable(List<Casualty> CasualtyList){			
			int[][] expectRPM=new int[CasualtyList.size()][Constants.HOSPITAL_COUNT];
			for(int i=0;i<CasualtyList.size();++i){
				for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
					Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//��ҽԺ������ѡȡһ�� ҽԺ����
					expectRPM[i][oneHospital.hid]=0;
					}
				
			}
			return expectRPM;
		}
		private Result chooseHospitalBasedOnRPM(List<Casualty> ambCasualtyList){
			//List<Object> ambulanceCasualtyList=new ArrayList<Object>();
			List<Object> hospitalResult=new ArrayList<Object>();
			 Result result = new Result();						
			
			int[][] expectRPM=new int[ambCasualtyList.size()][Constants.HOSPITAL_COUNT]; //������� expectRPM[��amb�е���Ա��ţ�������ԱcasualtyID][ҽԺID]
			//�õ� ���ȳ�����Ա�б��е���Ա���͵�����ҽԺʱ Ԥ�ڵ�rpm
			
			for(int i=0;i<ambCasualtyList.size();++i){
				for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
					Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//��ҽԺ������ѡȡһ�� ҽԺ����
					expectRPM[i][oneHospital.hid]=newExpectRPM(ambCasualtyList.get(i),oneHospital);//���� ��Աamb.casualtyList.get(i)���� ���͵� oneHospitalʱ Ԥ�ڵ�RPM
				}
				
			}
			
			//������͵�ÿ��ҽԺʱ  ���ȳ���������Ա��Ԥ��RPM֮�ͣ� ��ֻ��һ�ַ�ʽ����������� ��ǰ���ȳ��� RPM��͵���Ա��ѡ���� expectRPM����ҽԺ
			int[] sumCasualtyExpectRPM=new int[Constants.HOSPITAL_COUNT];//����ҽԺ��  ��ԱԤ��RPM֮��
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				sumCasualtyExpectRPM[i]=0;//ĳһ��ҽԺ Ԥ��RPM֮�� ��ʼֵΪ0  
				for(int j=0;j<ambCasualtyList.size();++j){
					sumCasualtyExpectRPM[i]+=expectRPM[j][i];// ���б��� ������Ա ���͵� ĳһҽԺʱԤ�ڵ�RPM֮��
				}
			}
			int index=0;//����Ŀ��ҽԺ��ţ����ѡȡһ����Ȼ�����
			//ѡȡsumCasualtyExpectRPM����ֵ
			int maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[index];// �����ѡȡ�� ҽԺ��Ӧ��RPM֮����Ϊ ���RPM
			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
				if(sumCasualtyExpectRPM[i]>maxSumCasualtyExpectRPM){
					maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[i];
					index=i;//ѡȡrpm֮������ҽԺ��Ϊ����ҽԺ
				}
			}
			
			//���sumCasualtyExpectRPM������ֵͬʱ����Ҫ�ȶ���һ����̬���ݣ��洢�����ֵ��ͬ��index
			ArrayList<Integer> indexList=new ArrayList<Integer>();
			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
				if(sumCasualtyExpectRPM[i]==maxSumCasualtyExpectRPM){
					indexList.add(i);
				}
			}
			Random r = new Random();
			int x=r.nextInt(indexList.size());
			index=(int)indexList.get(x);//�����ֵ��ͬ��index�б������ѡ��һ����Ϊ����Ŀ��
			
			//ѡȡҽԺ��hidΪindex�ģ���Ϊ���index�����ҽԺ��hid����
			for(int c=0; c<MCIContextBuilder.hospitalList.size(); ++c){
				Hospital oneHospital=MCIContextBuilder.hospitalList.get(c);
					if(oneHospital.hid==index){
						hospitalResult.add(oneHospital);						
					}
			}	
			Hospital tHospital=(Hospital)hospitalResult.get(0);	
			result.hos = tHospital;
			result.sum = maxSumCasualtyExpectRPM;
			return result;
			
		}
}