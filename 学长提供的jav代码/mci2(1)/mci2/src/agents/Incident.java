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
	public int x, y; // 事故发生地点坐标
	private boolean triageStart; // 分流开始标识，当第一辆急救车到达时，才开始进行伤员分流操作。


	// 伤员信息输出计数：死亡地点分析
	private int survivalNum; // 当前幸存人数
	private int deadNum;// 当前死亡人数
	private int beTriagedNum; // 被分流人数
	private int onIncident; // 还在急救现场的伤员
	private int onTheWaydNum; // 在后送途中人数
	private int onTheHospitaldNum; // 当前在院总人数
	private int dischargeNum;// 当前出院人数
	private int[] atHospitaldNum; // 到达医院人数,按照医院ID分布

	private double[] hospitalWaitEDTime;// 各个医院ED等待时间长度
	private double[] hospitalWaitICUTime;// 各个医院icu等待时间长度
	private double[] hospitalWaitGWTime;// 各个医院GW等待时间长度

	private int[] hospitalEDAvailableCount;// 各个医院ED可用资源
	private int[] hospitalICUAvailableCount;// 各个医院ICU可用资源
	private int[] hospitalGWAvailableCount;// 各个医院GW可用资源
	
	private double[][] hospitalTreatTime;// 各个医院ED处理时间长度 zyh  统计用
//	private double[] hospitalTreatICUTime;// 各个医院icu处理时间长度
//	private double[] hospitalTreatGWTime;// 各个医院GW处理时间长度

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

		this.survivalNum = 0;// 整体存活存活数量，初始值为0；
		this.beTriagedNum = 0;
		this.onIncident = 0;
		this.onTheWaydNum = 0;
		this.onTheHospitaldNum = 0;
		this.dischargeNum = 0;
		this.atHospitaldNum = new int[Constants.HOSPITAL_COUNT];
	}
	
	public void step() {

		double currentTime = MCIContextBuilder.currentTime;

		// 获取事件地点是否有急救车对象

		List<Ambulance> ambulanceArrived = new ArrayList<Ambulance>();
		List<Ambulance> bestAmbulanceList = new ArrayList<Ambulance>();  //最佳救护车组合 zyh
		for (int a=0; a<MCIContextBuilder.ambulanceList.size(); ++a) {			
			Ambulance cc = MCIContextBuilder.ambulanceList.get(a);									

			if (cc.positionFlag == Constants.POSITION_INCIDENT) {
				if (cc.casualtyList == null) { // 对到达现场之后没有装载病人的急救车进行，装载操作。
					ambulanceArrived.add(cc);
				} else if (cc.casualtyList.size() == 0) {
					ambulanceArrived.add(cc);
				}

			}
		}

		if (ambulanceArrived.size() == 0) { // 表示没有可以装载的急救车，包括没有到达的
			if (triageStart) { // 判断是否开始分流，true的时候，进行分流操作，在这里进行判断的话，当
				setTriageTag();
			} else {
				// 没有开始分流，什么都不做；
			}
		} else {// 表示有急救车到达
			System.out.println("现场有车"+ambulanceArrived.size());
			Integer remain = 0;
			if (triageStart) {
				remain = setTriageTag();
			} else {
				triageStart = true;// 判断是不是第一辆，如果是第一辆，将标志位置为true；
			}
			this.casualtyOnBoard = loadCasualty(ambulanceArrived);               //选取伤员
			//System.out.println("cas selected");
			//System.out.println(this.casualtyOnBoard.size());
			if(this.casualtyOnBoard.size()>0) {                                  //如果有伤员
				if(this.casualtyOnBoard.size()<Constants.AMBULANCE_CARRY_MAX*ambulanceArrived.size() && remain>0) {   //人数不足总运量且仍有未tag（避免车辆浪费）
					ambulanceArrived = ambulanceArrived.subList(0, (int) Math.ceil(this.casualtyOnBoard.size()/2));
				}
			bestAmbulanceList=new ArrayList<Ambulance>(allocateCas(this.casualtyOnBoard,ambulanceArrived));  //组合伤员取得最优
			//System.out.println("681 "+bestAmbulanceList.size());
			for(int ii=0; ii<bestAmbulanceList.size();++ii) {                  //分配
				Ambulance one = bestAmbulanceList.get(ii);
				//System.out.println("685: "+one.casualtyList.size());

					Ambulance cc = (Ambulance) ambulanceArrived.get(ii);
					//System.out.println("688: "+cc.casualtyList.size());

					if (cc.positionFlag == Constants.POSITION_INCIDENT) {                        //在现场的车
						if (cc.casualtyList == null) { // 对到达现场之后没有装载病人的急救车进行，装载操作。
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
								cc.casualtyList.get(jj).casualtyPositionFlag=Constants.POSITION_ONTHEWAY;//修改伤员位置信息为在路上
								cc.casualtyList.get(jj).loadAmbulanceTime=currentTime;//记录伤员被搭载上急救车的时间
								cc.casualtyList.get(jj).ambulanceID=cc.ambulanceID;//记录伤员搭载的急救车编号
							}
							System.out.println(out);
							cc.targetx=cc.target_hospital.x;
							cc.targety=cc.target_hospital.y;
							cc.positionFlag=Constants.POSITION_ONTHEWAY;//将急救车状态变为 在路上	，启动急救车
						}else {
							cc.positionFlag=Constants.POSITION_INCIDENT;
							System.out.println("Ambulance "+cc.ambulanceID+" wait at Incident");//输出
						}

					}
				
				
			}
			}
		}
		
		 if(currentTime==Constants.RUN_TIME){
		// 输出伤员统计数据
		// 输出伤员信息，到csv文件
		// 计算每一步，当前总幸存人数，当前现场已分流人数，当前后送途中人数，当前在院总人数，当前各医院在院人数，当前出院人数
			 for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
					Casualty oneCasualty = MCIContextBuilder.casualtyList.get(a);// 对所有的伤员对象进行遍历，如果
				// this.survivalNum++;// 当存在一个伤员对象，表明其还存活，在整体存活数量上+1
				if ((oneCasualty.casualtyPositionFlag == Constants.POSITION_INCIDENT)
						&& (oneCasualty.triageTag != null)) {
					this.beTriagedNum++;// 现场已分流人数
				}
				if (oneCasualty.casualtyPositionFlag == Constants.POSITION_INCIDENT) {
					this.onIncident++;
				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_ONTHEWAY) {// 伤员已经上路上了，记录其数量
					this.onTheWaydNum++;

				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// 伤员到达医院
					this.atHospitaldNum[oneCasualty.arrHospitalID]++;// 表明伤员到达其后送医院，后送医院响应的伤员数量+1；
				} else if (oneCasualty.casualtyPositionFlag == Constants.POSITION_DISCHARGE) {// 表示伤员已经出院
					this.dischargeNum++;
				} else {// 伤员已死亡
					this.deadNum++;//
				}
			}
		

		for (int ww = 0; ww < this.atHospitaldNum.length; ++ww) {
			this.onTheHospitaldNum += this.atHospitaldNum[ww];
		}
		this.survivalNum = Constants.CASUALTY_COUNT - this.deadNum;// 计算当前幸存人数

		try {
			File csv = new File("D:\\eclipse-workspace\\mci\\data\\casualty0824.csv");
			BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
			// bw.write("当前时间"+","+"当前幸存人数"+","+"当前现场人数"+"当前已分流人数"+","+"当前后送途中人数"+","+"当前在院总人数"+"\r\n");
			bw.newLine();
			String outline = currentTime + "," + this.survivalNum + ","
					+ this.onIncident + "," + this.beTriagedNum + ","
					+ this.onTheWaydNum + "," + this.onTheHospitaldNum + ","
					+ this.dischargeNum;
			bw.write(outline);
			bw.close();
			// 每一次输出之后，需要将计算变量清零，方便下次计数
			this.survivalNum = 0;// 整体存活存活数量，初始值为0；
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

//		// 计数伤员在医院的等待时间
//		for (Object obj : getGrid().getObjects()) {
//			if (obj instanceof Casualty) {
//				Casualty aCasualty = (Casualty) obj;
//				if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// 统计当前伤员在医院的等待时间
//					double waitEDTime = 0.0; // 初始值为0.0
//					double waitICUTime = 0.0; // 初始值为0.0
//					double waitGWTime = 0.0; // 初始值为0.0
//
//					if (aCasualty.enterEDTime != 0.0) {// 如果伤员进入ED，如果enterEDTime
//														// 为0.0
//														// 表示伤员进入了等待列表，此时暂不计算ED等待时间
//						waitEDTime = aCasualty.enterEDTime
//								- (aCasualty.arrHospitalTime);// 伤员等待进入ED的时间
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime != 0.0)
//							&& ((aCasualty.enterGWTime == 0.0))) {// 表示伤员进入ICU，还没有进入GW
//						// 计算等待进入ICU的时间
//						waitICUTime = aCasualty.enterICUTime
//								- aCasualty.enterEDTime;
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime == 0.0)
//							&& ((aCasualty.enterGWTime != 0.0))) {// 表示伤员进入GW，没有进入ICU
//						// 计算 从ED等待进入GW的时间
//						waitGWTime = aCasualty.enterGWTime
//								- aCasualty.enterEDTime;
//
//					}
//					if ((aCasualty.enterEDTime != 0.0)
//							&& (aCasualty.enterICUTime != 0.0)
//							&& ((aCasualty.enterGWTime != 0.0))) {// 表示伤员先进入ICU,然后再进入GW
//						// 计算 从ICU等待进入GW的时间
//						waitGWTime = aCasualty.enterGWTime
//								- aCasualty.leaveICUTime;
//					}
//
//					// 为该伤员所在医院的添加 等待时间
//					this.hospitalWaitEDTime[aCasualty.arrHospitalID] += waitEDTime;
//					this.hospitalWaitICUTime[aCasualty.arrHospitalID] += waitICUTime;
//					this.hospitalWaitGWTime[aCasualty.arrHospitalID] += waitGWTime;
//				}
//			}
//		}
//		// 计算各个医院的急救能力，主要是ED count,icu_count 和GW_count
//		for (Object obj : getGrid().getObjects()) {// 选取医院对象,并将当前的各项资源情况，赋值给数组
//			if (obj instanceof Hospital) {
//				Hospital aHospital = (Hospital) obj;
//				this.hospitalEDAvailableCount[aHospital.hid] = aHospital.ED_Available_Count;
//				this.hospitalICUAvailableCount[aHospital.hid] = aHospital.ICU_Avaible_Bed_Count;
//				this.hospitalGWAvailableCount[aHospital.hid] = aHospital.GW_Avaible_Bed_Count;
//			}
//		}

//		// 输出各个医院的急救能力和等待时间
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
//			// 每一次输出之后，需要将计算变量清零，方便下次计数
//			hospitalWaitEDTime = new double[Constants.HOSPITAL_COUNT];// 各个医院ED等待时间长度
//			hospitalWaitICUTime = new double[Constants.HOSPITAL_COUNT];// 各个医院icu等待时间长度
//			hospitalWaitGWTime = new double[Constants.HOSPITAL_COUNT];// 各个医院GW等待时间长度
//
//		} catch (Exception e) {
//			throw new IllegalArgumentException(String.format(e.toString()));
//
//		}
		
//		//统计处理时间 zyh
//		 this.hospitalTreatTime=getAvgTreatTime();
//		// 输出各个医院的急救能力 zyh
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
		// 急救车搭载伤员规则写在此处，输入急救车，返回其搭载的伤员列表
		// 选择所有已经tag的伤员
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
				//表示现场存在红色伤员，优先加载
				int index=0;	
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagRed.size());//从CasualtiesRed中随机选取一名伤员，如果不采取随机方式，采用何种方式选择伤员？？，这个就是
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagRed);// 选取RPM最小的 对象 作为搭载		
				}
				Object obj=casualtiesTagRed.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //伤员加载之后,将其位置信息修改为在路上
				casualtiesTagRed.remove(index); //伤员被装载之后将该伤员从红色列表中移走；
				temp++;
			}else if(casualtiesTagYellow.size()>0){
				int index=0;		
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagYellow.size());//从CasualtiesRed中随机选取一名伤员，如果不采取随机方式，采用何种方式选择伤员？？，这个就是
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagYellow);// 选取RPM最小的 对象 作为搭载	
				}
				Object obj=casualtiesTagYellow.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //伤员加载之后,将其位置信息修改为在路上
				casualtiesTagYellow.remove(index); //伤员被装载之后将该伤员从黄色列表中移走；
				temp++;
			}else if(casualtiesTagGreen.size()>0){
				int index=0;				
				if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_0){
					Random r = new Random();
					index=r.nextInt(casualtiesTagGreen.size());//从CasualtiesRed中随机选取一名伤员，如果不采取随机方式，采用何种方式选择伤员？？，这个就是
				}
				else if(Constants.LOADCASUALTY_MODE==Constants.LOADCASUALTY_MODE_1){
					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagGreen);// 选取RPM最小的 对象 作为搭载	
				}
				Object obj=casualtiesTagGreen.get(index);
				Casualty ss=(Casualty)obj;
				tempCasualtyList.add(ss);
				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //伤员加载之后,将其位置信息修改为在路上
				casualtiesTagGreen.remove(index); //伤员被装载之后将该伤员从绿色列表中移走；
				temp++;
			}else{
				temp++;// 现场没有可以装载的伤员；
			}

		} while (temp < Constants.AMBULANCE_CARRY_MAX*ambulanceArrived.size());
		//}
		return tempCasualtyList;

	}
	private int newChooseCasualtyIndexBasedOnRPM(List<Casualty> casualtyList){
		int index=0;//返回的索引值
		int minRPM=12;//最小RPM的初始值
			
		//筛选有价值伤员
		for(int i=0;i<casualtyList.size();++i){
			Casualty one=casualtyList.get(i);//将obj对象转为Casualty对象
			one.valuableOfRevise = false;
			int[] expectRPM=new int[Constants.HOSPITAL_COUNT]; //这个里面 expectRPM[是amb中的伤员序号，不是伤员casualtyID][医院ID]
			
			for(int j=0;j<MCIContextBuilder.hospitalList.size();++j){
				Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//从医院对象中选取一个 医院对象
				//expectRPM[oneHospital.hid]=newExpectRPMOnArriveHos(one,oneHospital);//计算 预期的RPM
				expectRPM[oneHospital.hid]=newExpectRPM(one,oneHospital);//计算 预期的RPM
				if(expectRPM[oneHospital.hid]>0) {
					one.valuableOfRevise = true;
					break;
				}
			}
			if((one.RPM<minRPM) &&(one.valuableOfRevise = true)){//当某一个对象的RPM小于最小RPM值时，将其值赋值给minRPM  只选择不会途中死亡的
				//if(one.RPM<minRPM){//当某一个对象的RPM小于最小RPM值时，将其值赋值给minRPM  只选择不会途中死亡的
				minRPM=one.RPM;
				index=i;//同时将此时的索引值赋值给 index 用于返回
			}
		}
		return index;
	}
	private List<Ambulance> allocateCas(List<Casualty> casualtyOnBoard2, List<Ambulance> ambulanceArrived) {
		List<Casualty> casualtylist = casualtyOnBoard2;
		List<Ambulance> ambulanceList = new ArrayList<Ambulance>();
		List<Ambulance> bestAmbulanceList = new ArrayList<Ambulance>();  //最佳救护车组合
		List<Casualty> onelist = new ArrayList<Casualty>();
		List<Casualty> twolist = new ArrayList<Casualty>();
		List<Casualty> threelist = new ArrayList<Casualty>();
		List<Casualty> fourlist = new ArrayList<Casualty>();
		List<Casualty> fivelist = new ArrayList<Casualty>();
		List<Casualty> sixlist = new ArrayList<Casualty>();
		List<Casualty> sevenlist = new ArrayList<Casualty>();
		List<Casualty> eightlist = new ArrayList<Casualty>();
		List<Casualty> ninelist = new ArrayList<Casualty>();
	
		
		//获取医院对象
		List<Hospital> hospitalList=MCIContextBuilder.hospitalList;	
		int index=0;
		int hospitalUsedCountList[]=new int[Constants.HOSPITAL_COUNT];//医院使用量列表 
		int hospitalEDUsedCountList[]=new int[Constants.HOSPITAL_COUNT];//医院急诊室使用量列表
		for(int c=0; c<hospitalList.size(); ++c){
				Hospital tt=hospitalList.get(c);
				//取出当前各个医院 总体的床位使用数量
				int usedCount=(Constants.ED_AVAILABLE_COUNT+Constants.ICU_AVAILABLE_COUNT+Constants.GW_AVAILABLE_COUNT)-(tt.ED_Available_Count+tt.ICU_Avaible_Bed_Count+tt.GW_Avaible_Bed_Count);
				//将当前 总体床位数量 保存到数组 相应医院编号下面
				hospitalUsedCountList[tt.hid]=usedCount;
				hospitalEDUsedCountList[tt.hid]=tt.ED_Available_Count;
			
		}
		
		if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_0){ //随机选择模式
	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
				//随机选择一家医院作为后送医院。
				Random r = new Random();
				index=r.nextInt(hospitalList.size());
				Hospital tHospital=(Hospital)hospitalList.get(index);
				t.target_hospital = tHospital;
				bestAmbulanceList.add(t);
			}
		}
		else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_1) { //已用床位数总和最小
			int min=hospitalUsedCountList[0];
			for(int i=0;i<hospitalUsedCountList.length;++i){
				if(min>hospitalUsedCountList[i]){
					min=hospitalUsedCountList[i];
					index=i;			
				}
			}

			//选取医院中hid为index的，因为这个index是针对医院的hid来的
			
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
			else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_2) {	//所有科室平均等待时间最短
			double[][] avgWaitTime=getAvgWaitTime();//获取当前每个医院，每个科室的平均等待时间
			double minAvgWaitTime=avgWaitTime[0][Constants.DEPT_ED]+avgWaitTime[0][Constants.DEPT_ICU]+avgWaitTime[0][Constants.DEPT_GW];
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				if(minAvgWaitTime>(avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW])){
					index=i;//将最小的 医院ID赋值给index
					minAvgWaitTime=avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW];
				}
			}
			
			//选取医院中hid为index的，因为这个index是针对医院的hid来的			
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
			else if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_5) { //最短距离
			List<Hospital> hospitalResult=new ArrayList<Hospital>();
			for(int i=0;i<hospitalEDUsedCountList.length;++i){
				if(hospitalEDUsedCountList[i]>0){
					index=i;	
					for(int c=0; c<hospitalList.size(); ++c){     //添加所有急诊室有空床位的
						Hospital oneHospital=hospitalList.get(c);
							if(oneHospital.hid==index){
								hospitalResult.add(oneHospital);						
							}
					}
				}
			}
			double min = 1000;
			int index2=0;
			if(hospitalResult!=null && hospitalResult.size()>0) { //有急诊有空床位的医院
				
				for(int i=0;i<hospitalResult.size();++i){     //有空床位的最近医院
					Hospital hospitalPosition=hospitalResult.get(i);//获取医院所在位置
					double dis=Math.abs(Constants.MCI_X-hospitalPosition.x)+Math.abs(Constants.MCI_Y-hospitalPosition.y);  //计算 伤员与医院之间的距离					
					if (dis<min) {
						min=dis;
						index2=i;				
					}			
				}			
			}else { //没有则随机选
				Random r = new Random();
				index2=r.nextInt(hospitalResult.size());
			}
										
			Hospital tHospital=hospitalResult.get(index2);	
			for(int ii=0;ii<ambulanceArrived.size();++ii){
				Ambulance t=ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//		int bestRPM = 0;                                      //最佳RPM
//		//for(int i=0;i<ambulanceArrived.size();++i){
//		if(ambulanceArrived.size()==1) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);
//			one.casualtyList=casualtylist;
//			one.target_hospital = chooseHospitalBasedOnRPM(one.casualtyList).hos;
//			bestAmbulanceList.add(one);
//		}
//		if(ambulanceArrived.size()==2) {
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
//				//System.out.println("1035: "+s.casualtyID);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
//					//System.out.println("1056: "+ss.casualtyID);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
//							ambulanceList.add(one);
//							ambulanceList.add(two);
//							ambulanceList.add(three);
//							ambulanceList.add(four);
//							System.out.println("one size: "+one.casualtyList.size());
//							System.out.println("two size: "+two.casualtyList.size());
//							System.out.println("three size: "+three.casualtyList.size());
//							System.out.println("four size: "+four.casualtyList.size());
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
//			Ambulance two=(Ambulance)ambulanceArrived.get(1);
//			Ambulance three=(Ambulance)ambulanceArrived.get(2);
//			Ambulance four=(Ambulance)ambulanceArrived.get(3);
//			Ambulance five=(Ambulance)ambulanceArrived.get(4);
//			one.casualtyList = new ArrayList<Casualty>();
//			two.casualtyList = new ArrayList<Casualty>();
//			three.casualtyList = new ArrayList<Casualty>();
//			four.casualtyList = new ArrayList<Casualty>();
//			five.casualtyList = new ArrayList<Casualty>();
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//											for(int i5=0;i5<casualtylist.size();++i5){//第一个格子 5车
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//												for(int j5=0;j5<casualtylist.size();++j5){//第二个格子
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
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
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//											for(int i5=0;i5<casualtylist.size();++i5){//第一个格子 5车
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//												for(int j5=0;j5<casualtylist.size();++j5){//第二个格子
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//													for(int i6=0;i6<casualtylist.size();++i6){//第一个格子 6车
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//														for(int j6=0;j6<casualtylist.size();++j6){//第二个格子
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
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
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//											for(int i5=0;i5<casualtylist.size();++i5){//第一个格子 5车
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//												for(int j5=0;j5<casualtylist.size();++j5){//第二个格子
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//													for(int i6=0;i6<casualtylist.size();++i6){//第一个格子 6车
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//														for(int j6=0;j6<casualtylist.size();++j6){//第二个格子
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//															for(int i7=0;i7<casualtylist.size();++i7){//第一个格子 7车
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																for(int j7=0;j7<casualtylist.size();++j7){//第二个格子
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
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
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//											for(int i5=0;i5<casualtylist.size();++i5){//第一个格子 5车
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//												for(int j5=0;j5<casualtylist.size();++j5){//第二个格子
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//													for(int i6=0;i6<casualtylist.size();++i6){//第一个格子 6车
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//														for(int j6=0;j6<casualtylist.size();++j6){//第二个格子
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//															for(int i7=0;i7<casualtylist.size();++i7){//第一个格子 7车
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																for(int j7=0;j7<casualtylist.size();++j7){//第二个格子
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																	for(int i8=0;i8<casualtylist.size();++i8){//第一个格子 8车
//																		Casualty s8 = casualtylist.get(i8);
////																		s8.used = false;
////																		for(int ii=0;ii<ambulanceArrived.size();++ii){
////																			Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																		for(int j8=0;j8<casualtylist.size();++j8){//第二个格子
//																			Casualty ss8 = casualtylist.get(j8);
////																			ss8.used = false;
////																			for(int ii=0;ii<ambulanceArrived.size();++ii){
////																				Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																			for(int i9=0;i9<casualtylist.size();++i9){//第一个格子 9车
//																				Casualty s9 = casualtylist.get(i9);
////																				s9.used = false;
////																				for(int ii=0;ii<ambulanceArrived.size();++ii){
////																					Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																				for(int j9=0;j9<casualtylist.size();++j9){//第二个格子
//																					Casualty ss9 = casualtylist.get(j9);
////																					ss9.used = false;
////																					for(int ii=0;ii<ambulanceArrived.size();++ii){
////																						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
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
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
//			Ambulance one=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//			for(int i=0;i<casualtylist.size();++i){//第一个格子 1车
//				Casualty s = casualtylist.get(i);
////				s.used = false;
////				for(int ii=0;ii<ambulanceArrived.size();++ii){
////					Ambulance t=(Ambulance)ambulanceArrived.get(0);//将obj对象转为Ambulance对象
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
//				for(int j=0;j<casualtylist.size();++j){//第二个格子
//					Casualty ss = casualtylist.get(j);
////					ss.used = false;
////					for(int ii=0;ii<ambulanceArrived.size();++ii){
////						Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//					for(int i2=0;i2<casualtylist.size();++i2){//第一个格子 2车
//						Casualty s2 = casualtylist.get(i2);
////						s2.used = false;
////						for(int ii=0;ii<ambulanceArrived.size();++ii){
////							Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//						for(int j2=0;j2<casualtylist.size();++j2){//第二个格子
//							Casualty ss2 = casualtylist.get(j2);
////							ss2.used = false;
////							for(int ii=0;ii<ambulanceArrived.size();++ii){
////								Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							for(int i3=0;i3<casualtylist.size();++i3){//第一个格子 3车
//								Casualty s3 = casualtylist.get(i3);
////								s3.used = false;
////								for(int ii=0;ii<ambulanceArrived.size();++ii){
////									Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//								for(int j3=0;j3<casualtylist.size();++j3){//第二个格子
//									Casualty ss3 = casualtylist.get(j3);
////									ss3.used = false;
////									for(int ii=0;ii<ambulanceArrived.size();++ii){
////										Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//									for(int i4=0;i4<casualtylist.size();++i4){//第一个格子 4车
//										Casualty s4 = casualtylist.get(i4);
////										s4.used = false;
////										for(int ii=0;ii<ambulanceArrived.size();++ii){
////											Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//										for(int j4=0;j4<casualtylist.size();++j4){//第二个格子
//											Casualty ss4 = casualtylist.get(j4);
////											ss4.used = false;
////											for(int ii=0;ii<ambulanceArrived.size();++ii){
////												Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//											for(int i5=0;i5<casualtylist.size();++i5){//第一个格子 5车
//												Casualty s5 = casualtylist.get(i5);
////												s5.used = false;
////												for(int ii=0;ii<ambulanceArrived.size();++ii){
////													Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//												for(int j5=0;j5<casualtylist.size();++j5){//第二个格子
//													Casualty ss5 = casualtylist.get(j5);
////													ss5.used = false;
////													for(int ii=0;ii<ambulanceArrived.size();++ii){
////														Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//													for(int i6=0;i6<casualtylist.size();++i6){//第一个格子 6车
//														Casualty s6 = casualtylist.get(i6);
////														s6.used = false;
////														for(int ii=0;ii<ambulanceArrived.size();++ii){
////															Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//														for(int j6=0;j6<casualtylist.size();++j6){//第二个格子
//															Casualty ss6 = casualtylist.get(j6);
////															ss6.used = false;
////															for(int ii=0;ii<ambulanceArrived.size();++ii){
////																Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//															for(int i7=0;i7<casualtylist.size();++i7){//第一个格子 7车
//																Casualty s7 = casualtylist.get(i7);
////																s7.used = false;
////																for(int ii=0;ii<ambulanceArrived.size();++ii){
////																	Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																for(int j7=0;j7<casualtylist.size();++j7){//第二个格子
//																	Casualty ss7 = casualtylist.get(j7);
////																	ss7.used = false;
////																	for(int ii=0;ii<ambulanceArrived.size();++ii){
////																		Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																	for(int i8=0;i8<casualtylist.size();++i8){//第一个格子 8车
//																		Casualty s8 = casualtylist.get(i8);
////																		s8.used = false;
////																		for(int ii=0;ii<ambulanceArrived.size();++ii){
////																			Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//																		for(int j8=0;j8<casualtylist.size();++j8){//第二个格子
//																			Casualty ss8 = casualtylist.get(j8);
////																			ss8.used = false;
////																			for(int ii=0;ii<ambulanceArrived.size();++ii){
////																				Ambulance t=(Ambulance)ambulanceArrived.get(ii);//将obj对象转为Ambulance对象
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
//							int sumRPM  = 0;            //RPM之和
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
//							for(int k=0;k<ambulanceList.size();++k){  //开始这个组合的判断
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
				System.out.println("有两辆车啦");
				int[][] matlabR1 = giveRPMTable(casualtylist);
				int matlabn1 = ambulanceArrived.size();
				int[][] matlabR2 = giveflyRPMTable(casualtylist);
				int matlabn2 = 0;
				Object[] result = null; // 用于保存计算结果
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
			         // 释放本地资源
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
	private int newExpectRPM(Casualty casualty, Hospital hospital){     //使用当前的RPM来计算预期而不是初始 zyh
		//选取casualty对象
	//	List<Object> localCasualty=new ArrayList<Object>(); //函数内部使用的伤员列表，
				
		int expectRPM=0;// 伤员预期RPM
		double currentTime = MCIContextBuilder.currentTime;
		//double[][] allHospitalAvgWaitTime=getAvgWaitTime();//获取当前每个医院中，每个科室的平均等待时间
		double[][] allHospitalAvgWaitTime=getExpWaitTime2();//使用新的预期等待时间    zyh
		//Casualty aCasualty=(Casualty)localCasualty.get(0);//获取 第一个元素  里面只有一个元素，表示传入的伤员对象
		//Hospital aHospital=(Hospital)localHospital.get(0);//获取 第一个元素  只有一个元素 表示传入的医院对象
		//获取从现场到医院时的距离
		double dis=Math.abs(Constants.MCI_X-hospital.x)+Math.abs(Constants.MCI_Y-hospital.y);  //计算 伤员与医院之间的距离
		double travelTime=Math.ceil(dis/Constants.AMBULANCE_TRAVEL_SPEED);   //计算 伤员运送到医院的时间
		//计算预期RPM
		//expectRPM=getCurrentRPM(currentTime+travelTime,casualty.InitialRPM);
		expectRPM=getCurrentRPM(currentTime-casualty.triageTime+travelTime,casualty.InitialRPM); //检伤时的rpm并不是初始rpm  所以等待时间就是此时时间
//		expectRPM=getCurrentRPM(travelTime,casualty.InitialRPM);
		if(expectRPM<5){//表示伤员到达医院之后需要进入ICU
			//修正expectRPM
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//修正等待时间为 后送时间+在该医院ICU的等待时间
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//修正等待时间为 后送时间+在该医院ICU的等待时间
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
			
		}else if(expectRPM<9){//表示伤员到达医院之后需要进入GW
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//修正等待时间为 后送时间+在该医院ICU的等待时间
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//修正等待时间为 后送时间+在该医院ICU的等待时间
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
			
		}else{//表示伤员到达医院之后需要进入ED
			double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//修正等待时间为 后送时间+在该医院ICU的等待时间
			//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//修正等待时间为 后送时间+在该医院ICU的等待时间
			expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);		
		}
		
		return expectRPM;//返回
		
		
	}
	//根据排队人数预期等待时间2  zyh //经验值：2 4 3
	 private double[][] getExpWaitTime2(){
		 double[][] avgTreatTime = getAvgTreatTime(); //平均治疗时间
		 double[][] expWaitTime = new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];
		 int[][] queNum = new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //当前排队人数
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
					if(queNum[i][j]==0){//如果排队伤员数量为0
						expWaitTime[i][j]=0;//直接赋值为0
					}else{
						expWaitTime[i][j]=avgTreatTime[i][j]*queNum[i][j];
					}
					
				}
			}
		 //ICU的等待时间加入ED的等待时间，GW同理
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
	 //获取平均治疗时间  zyh
	 private double[][] getAvgTreatTime(){
		 double[][] treatTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//记录每个医院，每个科室的总体治疗时间
			int[][]    treatedCasualty=new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//记录每个医院，每个科室处理的伤员人数。
			double[][] avgTreatTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //平均治疗时间
			
			for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
				Casualty aCasualty = MCIContextBuilder.casualtyList.get(a);
					//if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// 统计当前伤员在医院的等待时间  只统计当前在院伤员的等待时间，忽略了已经出院和在院内死亡的伤员的等待时间
					if (aCasualty.arrHospitalTime!=0.0) {// 伤员到达医院时间不为0，表明伤员被后送到过某医院。拓展了伤员的范围，不仅包括当前在院的伤员，还包括已经出院的伤员，以及在院内死亡的伤员
								
						double treatEDTime = 0.0; // 初始值为0.0
						double treatICUTime = 0.0; // 初始值为0.0
						double treatGWTime = 0.0; // 初始值为0.0
						if (aCasualty.overEDTime != 0.0) {// 如果伤员进入ED，如果enterEDTime
															
							treatEDTime = aCasualty.overEDTime
									- (aCasualty.enterEDTime);// 伤员进入ED的时间
							//计算在arrHospitalID 的ED 中接受过治疗的伤员
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ED]++;
						}
						if (aCasualty.overICUTime != 0.0)
								 {// 表示伤员进入ICU
							// 计算进入ICU的时间
							treatICUTime = aCasualty.overICUTime
									- aCasualty.enterICUTime;
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
						}
						if (aCasualty.overICUTime != 0.0)
								 {// 表示伤员进入GW
							// 计算 进入GW的时间
							treatGWTime = aCasualty.overGWTime
									- aCasualty.enterGWTime;
							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;

						}
						

						// 为该伤员所在医院的添加 等待时间
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_ED] += treatEDTime/Constants.ED_AVAILABLE_COUNT;
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_ICU] += treatICUTime/Constants.ICU_AVAILABLE_COUNT;
						treatTime[aCasualty.arrHospitalID][Constants.DEPT_GW] += treatGWTime/Constants.GW_AVAILABLE_COUNT;
					}
				
			}
			
			//计算每个医院，每个科室的平均等待时间
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				for(int j=0;j<Constants.DEPT_COUNT;++j){
					if(treatedCasualty[i][j]==0){//如果处理伤员数量为0
						avgTreatTime[i][j]=0;//直接赋值为0
					}else{
						avgTreatTime[i][j]=treatTime[i][j]/treatedCasualty[i][j];
					}
					
				}
			}
			return avgTreatTime;
		 
	 }
	// 对伤员进行分流操作
	private Integer setTriageTag() {
		// 先选取所有casualty对象，然后为其中没有tag的伤员，按照RPM分配tag，每次指定完成Constants.TRIAGE_NUM_AT_ONE_TIME数量的伤员。效率不高
		List<Casualty> casualtiesIncidentNotTag = new ArrayList<Casualty>();
		for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
			Casualty dd = MCIContextBuilder.casualtyList.get(a);
				if ((dd.casualtyPositionFlag == Constants.POSITION_INCIDENT)
						&& (dd.triageTag == null)) {
					casualtiesIncidentNotTag.add(dd);// 选取所有现场的没有tag的casualty对象
				}
		}

		if (casualtiesIncidentNotTag.size() > 0) {
			int temp = 0;
			double currentTime = MCIContextBuilder.currentTime;
			do {
				Random r = new Random();
        		
				int index = r.nextInt(casualtiesIncidentNotTag.size());// 从Casualties中随机选取一名伤员
				Object obj = casualtiesIncidentNotTag.get(index);
				Casualty aa = (Casualty) obj; // 将对象转换为Casualty对象
				int casualtyIniRPM = getInitialRPM(aa.RPM,currentTime);  //使用初始RPM检伤分类而不是此时的  zyh
				//int casualtyIniRPM = aa.RPM;
				temp++;
				String tag = null;
				if (casualtyIniRPM == 0 || aa.RPM == 0) { // 伤员的RPM等于0，表明伤员已经死亡
					tag = Constants.TAG_BLACK;
				} else if ((casualtyIniRPM > 0) && (casualtyIniRPM < 5)) { // 伤员RPM在1-4
																			// 之间,标识为red
					tag = Constants.TAG_RED;
				} else if ((casualtyIniRPM > 4) && (casualtyIniRPM < 9)) { // 伤员RPM
																			// 在5-8之间，标识为yellow
					tag = Constants.TAG_YELLOW;
				} else {
					tag = Constants.TAG_GREEN;
				}
				aa.triageTag = tag;// 将标签，赋值给该casualty对象
				aa.triageTime = currentTime;
				System.out.println("Casualty " + aa.casualtyID + " tag "
						+ aa.triageTag.toString());// 如果伤员已经死亡需要进行记录。
				casualtiesIncidentNotTag.remove(aa);
			} while (temp < Constants.TRIAGE_NUM_AT_ONE_TIME && casualtiesIncidentNotTag.size()>0);

		}
		return casualtiesIncidentNotTag.size();

	}
	private int getInitialRPM(int cRPM, double t) {        //根据现在的rpm查找初始rpm
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
	/*根据受伤时间长短，和初始RPM，返回伤员现在的RPM*/
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
	//获取平均等待时间数组，包含每个医院，每个科室的平均等待时间
	 private double[][] getAvgWaitTime(){
				double[][] waitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//记录每个医院，每个科室的总体等待时间
				int[][]    treatedCasualty=new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//记录每个医院，每个科室处理的伤员人数。
				double[][] avgWaitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //平均等待时间
				// 计数伤员在医院的等待时间
				
						for(int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {
			        		Casualty aCasualty = MCIContextBuilder.casualtyList.get(a);
						
						//if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// 统计当前伤员在医院的等待时间  只统计当前在院伤员的等待时间，忽略了已经出院和在院内死亡的伤员的等待时间
						if (aCasualty.arrHospitalTime!=0.0) {// 伤员到达医院时间不为0，表明伤员被后送到过某医院。拓展了伤员的范围，不仅包括当前在院的伤员，还包括已经出院的伤员，以及在院内死亡的伤员
									
							double waitEDTime = 0.0; // 初始值为0.0
							double waitICUTime = 0.0; // 初始值为0.0
							double waitGWTime = 0.0; // 初始值为0.0
							if (aCasualty.enterEDTime != 0.0) {// 如果伤员进入ED，如果enterEDTime
																// 为0.0
																// 表示伤员进入了等待列表，此时暂不计算ED等待时间
								waitEDTime = aCasualty.enterEDTime
										- (aCasualty.arrHospitalTime);// 伤员等待进入ED的时间
								//计算在arrHospitalID 的ED 中接受过治疗的伤员
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ED]++;
							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime != 0.0)
									&& ((aCasualty.enterGWTime == 0.0))) {// 表示伤员进入ICU，还没有进入GW
								// 计算等待进入ICU的时间
								waitICUTime = aCasualty.enterICUTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime == 0.0)
									&& ((aCasualty.enterGWTime != 0.0))) {// 表示伤员进入GW，没有进入ICU
								// 计算 从ED等待进入GW的时间
								waitGWTime = aCasualty.enterGWTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;

							}
							if ((aCasualty.enterEDTime != 0.0)
									&& (aCasualty.enterICUTime != 0.0)
									&& ((aCasualty.enterGWTime != 0.0))) {// 表示伤员先进入ICU,然后再进入GW
								// 计算 从ICU等待进入GW的时间
								waitGWTime = aCasualty.enterGWTime
										- aCasualty.leaveICUTime;
							
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;
								
								waitICUTime = aCasualty.enterICUTime
										- aCasualty.enterEDTime;
								treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
							}

							// 为该伤员所在医院的添加 等待时间
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_ED] += waitEDTime;
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_ICU] += waitICUTime;
							waitTime[aCasualty.arrHospitalID][Constants.DEPT_GW] += waitGWTime;
						}
					
				}
				
				//计算每个医院，每个科室的平均等待时间
				for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
					for(int j=0;j<Constants.DEPT_COUNT;++j){
						if(treatedCasualty[i][j]==0){//如果处理伤员数量为0
							avgWaitTime[i][j]=0;//直接赋值为0
						}else{
							avgWaitTime[i][j]=waitTime[i][j]/treatedCasualty[i][j];
						}
						
					}
				}
				return avgWaitTime;//返回平均等待时间列表
				
				
			}
		class Result {      //给下面的函数传递结果用
		    Hospital hos;
		    int sum;
		}
		private int[][] giveRPMTable(List<Casualty> CasualtyList){
					int[][] expectRPM=new int[CasualtyList.size()][Constants.HOSPITAL_COUNT];
//					System.out.println("4152 "+expectRPM.length);//
					for(int i=0;i<CasualtyList.size();++i){
						for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
							Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//从医院对象中选取一个 医院对象
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
					Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//从医院对象中选取一个 医院对象
					expectRPM[i][oneHospital.hid]=0;
					}
				
			}
			return expectRPM;
		}
		private Result chooseHospitalBasedOnRPM(List<Casualty> ambCasualtyList){
			//List<Object> ambulanceCasualtyList=new ArrayList<Object>();
			List<Object> hospitalResult=new ArrayList<Object>();
			 Result result = new Result();						
			
			int[][] expectRPM=new int[ambCasualtyList.size()][Constants.HOSPITAL_COUNT]; //这个里面 expectRPM[是amb中的伤员序号，不是伤员casualtyID][医院ID]
			//得到 急救车上伤员列表中的伤员运送到各个医院时 预期的rpm
			
			for(int i=0;i<ambCasualtyList.size();++i){
				for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
					Hospital oneHospital=MCIContextBuilder.hospitalList.get(j);//从医院对象中选取一个 医院对象
					expectRPM[i][oneHospital.hid]=newExpectRPM(ambCasualtyList.get(i),oneHospital);//计算 伤员amb.casualtyList.get(i)对象 后送到 oneHospital时 预期的RPM
				}
				
			}
			
			//计算后送到每个医院时  急救车上所有伤员的预期RPM之和， 这只是一种方式，还可以针对 当前急救车上 RPM最低的伤员，选择其 expectRPM最大的医院
			int[] sumCasualtyExpectRPM=new int[Constants.HOSPITAL_COUNT];//各个医院的  伤员预期RPM之和
			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
				sumCasualtyExpectRPM[i]=0;//某一个医院 预期RPM之和 初始值为0  
				for(int j=0;j<ambCasualtyList.size();++j){
					sumCasualtyExpectRPM[i]+=expectRPM[j][i];// 求列表中 所有伤员 后送到 某一医院时预期的RPM之和
				}
			}
			int index=0;//后送目标医院序号，随机选取一个，然后根据
			//选取sumCasualtyExpectRPM最大的值
			int maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[index];// 将随机选取的 医院对应的RPM之和作为 最大RPM
			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
				if(sumCasualtyExpectRPM[i]>maxSumCasualtyExpectRPM){
					maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[i];
					index=i;//选取rpm之和最大的医院作为后送医院
				}
			}
			
			//如果sumCasualtyExpectRPM存在相同值时，需要先定义一个动态数据，存储与最大值相同的index
			ArrayList<Integer> indexList=new ArrayList<Integer>();
			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
				if(sumCasualtyExpectRPM[i]==maxSumCasualtyExpectRPM){
					indexList.add(i);
				}
			}
			Random r = new Random();
			int x=r.nextInt(indexList.size());
			index=(int)indexList.get(x);//从最大值相同的index列表中随机选择一个作为后送目标
			
			//选取医院中hid为index的，因为这个index是针对医院的hid来的
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