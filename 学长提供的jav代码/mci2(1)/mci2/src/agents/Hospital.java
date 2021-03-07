package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import common.Constants;
import main.MCIContextBuilder;


public class Hospital {
	public int x, y; // 医院位置坐标 ,其在Grid上面的位置
	public int hid; // 医院的唯一编号
	public List<Casualty> hospitalCasualtyList; // 到达医院的伤员列表
	//static ISchedule schedule;// 时间记录需要
 
	public int ED_Available_Count;// 医院ED可用数量
	public List<Casualty> EDCasualtyList;// 医院ED员列表
	public List<Casualty> waitEDCasualtyList;// 等待ED伤员列表

	public int ICU_Avaible_Bed_Count;// ICU可用床位数量
	public List<Casualty> ICUCasualtyList;// ICU伤员列表
	public List<Casualty> waitICUCasualtyList;// 等待ICU伤员

	public int GW_Avaible_Bed_Count;// GW可用床位数量
	public List<Casualty> GWCasualtyList;// GW伤员列表
	public List<Casualty> waitGWCasualtyList;// 等待GW列表
	
	public double exp_ED_Wait_Time;  //预期各部门等待时间
	public double exp_ICU_Wait_Time;
	public double exp_GW_Wait_Time;
	
	public double Avg_ED_Wait_Time;  //平均各部门等待时间
	public double Avg_ICU_Wait_Time;
	public double Avg_GW_Wait_Time;
	
	public double ED_Wait_Time;  //既往各部门等待时间
	public double ICU_Wait_Time;
	public double GW_Wait_Time;

	public Hospital(final int x, final int y, final int id) {
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
		this.hid = id;
		this.hospitalCasualtyList = null;
		this.ED_Available_Count = Constants.ED_AVAILABLE_COUNT;// 初始化ED数量
		this.ICU_Avaible_Bed_Count = Constants.ICU_AVAILABLE_COUNT;// 初始化ICU数量
		this.GW_Avaible_Bed_Count = Constants.GW_AVAILABLE_COUNT;// 初始化GW数量
	}

	public int getID() {
		return this.hid;
	}


	public void step() {

		double currentTime=MCIContextBuilder.currentTime;
		
		//ED等待列表，先对其进行判断，保证，如果里面有等待伤员的话，先安排其进入ED
		if (this.waitEDCasualtyList != null) {
			if (this.waitEDCasualtyList.size() > 0) {// 列表不为空，表明有等待进入ED的伤员
				// 将伤员从waiEDCasualtyList转移到EDCasualtyList中
				int mm = 0;
				do {
					if(this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyPositionFlag==Constants.POSITION_DEAD){//如果伤员已经死亡
						//输出死亡信息
						String s = "Dead:Hospital " + this.hid + " Casualty "
								+ this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyID
								+ " Dead waiting ED at" + currentTime;// 输出伤员位置
						System.out.println(s);	
						this.waitEDCasualtyList.remove(this.waitEDCasualtyList.size()-1);//从等待列表中释放该伤员
					}else{
						if (ED_Available_Count > 0) {
							//需要处理在等待时候死亡的伤员
							EDCasualtyList = getCasualty(
									this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1),//选取等候队列中，最先排队的伤员
									this.EDCasualtyList);
							this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).enterEDTime = currentTime;// 记录伤员进入ED的时间
							ED_Available_Count--;// 占用ED资源
							String s = "Hospital " + this.hid + " Casualty "
									+ this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyID
									+ " Enter the ED at" + currentTime;// 输出伤员位置
							System.out.println(s);
							this.waitEDCasualtyList.remove(this.waitEDCasualtyList.size()-1);
						} else {
							mm++;// 表示目前ED中已满，waitEDList中有mm个伤员需要等待
							// break;
						}
					}	
					
				} while (this.waitEDCasualtyList.size() > mm); // 循环将waitEDCasualtyList中的对象，添加到EDCasualtyList中，当EDcount满了则不再添加

			}
		}
		//医院伤员列表，在安排完waitEDList之后再判断是否有新进入的伤员，如果新进入的还是需要等待的话，则将其插入waitEDList等待下一步时候，判断其去向
		if (this.hospitalCasualtyList != null) {
			if (this.hospitalCasualtyList.size() > 0) {// 表示医院有伤员 进入伤员转移流程
				int jj = 0;
				do {
					if (ED_Available_Count > 0) {// 表明ED还可以接受病人，将伤员添加到ED
						EDCasualtyList = getCasualty(
								this.hospitalCasualtyList.get(jj),
								this.EDCasualtyList);
						this.hospitalCasualtyList.get(jj).enterEDTime = currentTime;// 记录伤员进入ED的时间
						ED_Available_Count--;// 占用ED资源
						// 输出伤员位置

						String s = "Hospital " + this.hid + " Casualty "
								+ this.hospitalCasualtyList.get(jj).casualtyID
								+ " Enter the ED at" + currentTime;// 输出伤员位置
						System.out.println(s);

						this.hospitalCasualtyList.remove(jj);// 将该伤员从HospitalList中移除

					} else {
						waitEDCasualtyList = getCasualty(
								this.hospitalCasualtyList.get(jj),
								this.waitEDCasualtyList);
//						String s = "Wait: Hospital " + this.hid + " Casualty "
//								+ this.hospitalCasualtyList.get(jj).casualtyID
//								+ " wait ED at" + currentTime;// 输出伤员位置
//						System.out.println(s);

						this.hospitalCasualtyList.remove(jj);
					}

				} while (this.hospitalCasualtyList.size() > jj);// 通过减少size的大小进行调节，jj=0不变，表示，只要hospitalCasualtyList中有伤员就进行循环，直到HospitalList中没有伤员
			}
		}

		// ED进入，入口，用于完成ED中的所有操作	
		if (this.EDCasualtyList != null) {
			if (this.EDCasualtyList.size() > 0) {
				int ss = 0;//
				do {
					if (this.EDCasualtyList.get(ss).RPM < 5) {// 表明伤员伤情严重，需要进入ICU
						if (this.ICU_Avaible_Bed_Count > 0) {// 表明ICU有空闲床位，转移伤员到ICUCasualtyList中
							this.ICUCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.ICUCasualtyList);
							this.EDCasualtyList.get(ss).enterICUTime = currentTime;// 记录伤员进入ICU时间
							this.ICU_Avaible_Bed_Count--;// 占用ICU资源；
							this.ED_Available_Count++;// 释放ED资源
							// 输出伤员信息
							String s = "Hospital " + this.hid + " Casualty "
									+ this.EDCasualtyList.get(ss).casualtyID
									+ " Enter the ICU at" + currentTime;// 输出伤员位置
							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList移除伤员
						} else {// ICU没有空位，将伤员转移到waiICUCasualtyList中,但是不释放ED资源
							this.waitICUCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.waitICUCasualtyList);
							// 输出伤员信息
//							String s = "Wait: Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " wait the ICU at" + currentTime;// 输出伤员位置
//							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList移除伤员
						}

					} else if (this.EDCasualtyList.get(ss).RPM < 9) {// 伤员在5-8之间，其需要进入GW
						if (this.GW_Avaible_Bed_Count > 0) {// GW有空床位,将伤员转移到GWCasualtyList中，同时释放ED资源
							this.GWCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.GWCasualtyList);
							this.EDCasualtyList.get(ss).enterGWTime = currentTime;// 记录伤员进入GW时间
							this.GW_Avaible_Bed_Count--;// 占用GW资源
							this.ED_Available_Count++;// 释放ED资源

							// 输出伤员位置信息
							String s = "Hospital " + this.hid + " Casualty "
									+ this.EDCasualtyList.get(ss).casualtyID
									+ " Enter the GW at" + currentTime;// 输出伤员位置
							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList移除伤员

						} else {
							this.waitGWCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.waitGWCasualtyList);
							// 输出伤员位置信息
//							String s = "Wait: Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " wait the GW at" + currentTime;// 输出伤员位置
//							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList移除伤员
						}
					} else if (this.EDCasualtyList.get(ss).RPM == 12) {// 伤员RPM达到12时，其出院
						this.EDCasualtyList.get(ss).stopDeterioration=true;//出院病人停止伤情恶化
						this.EDCasualtyList.get(ss).dischargeTime = currentTime;// 记录伤员出院时间
						this.EDCasualtyList.get(ss).overEDTime = currentTime;//记录处理时间 zyh
						this.EDCasualtyList.get(ss).leaveEDTime = currentTime;//记录停留时间 zyh
						this.EDCasualtyList.get(ss).casualtyPositionFlag = Constants.POSITION_DISCHARGE;// 改变伤员位置状态为
																										// 出院
						String s = "Discharge:Hospital " + this.hid
								+ " Casualty "
								+ this.EDCasualtyList.get(ss).casualtyID
								+ " discharge at" + currentTime;// 输出伤员位置
						System.out.println(s);

						this.EDCasualtyList.remove(ss);// 将伤员从ED列表中释放，表明其从ED出院
						this.ED_Available_Count++;// 释放ED资源
						// 记录伤员出院时间
					} else {// 伤员伤情在9-12之间，在ED中接受一段时间的治疗就能恢复
							// 表明，停止伤情恶化
							// 判断其在ED中的停留时间，是否达到ED的平均处理时间，到达时将其RPM赋值为12，表明伤员在ED中一段时间后，出院，平均时间按照15——20min取随机数
						this.EDCasualtyList.get(ss).stopDeterioration = true;// 停止伤情恶化
						double EDtreatTimeDuration = currentTime
								- this.EDCasualtyList.get(ss).enterEDTime;
						Random r = new Random();
						double randomTime = r.nextDouble()*3+5;  //5~8
						if (EDtreatTimeDuration > randomTime) {// 表明当伤员在ED中 每
																// 5-8分钟好转
																// 1个点的RPM
							this.EDCasualtyList.get(ss).RPM += 1;
						} else {
							// 表明伤员还在ED中接受处置
//							String s = "Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " treat at ED for" + EDtreatTimeDuration;// 输出伤员位置
//							System.out.println(s);
							ss++;
						}
						
						

					}
				} while (this.EDCasualtyList.size() > ss);
			}
		}

		// ICU 等待列表
		if (this.waitICUCasualtyList != null) {
			if (this.waitICUCasualtyList.size() > 0) {
				int xx = 0;
				do {
					if(this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyPositionFlag==Constants.POSITION_DEAD){
						// 输出伤员死亡信息
						String s = "Dead: Hospital " + this.hid
								+ " Casualty "
								+ this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyID
								+ " Dead waiting GW at" + currentTime;// 输出伤员位置
						System.out.println(s);
						this.ED_Available_Count++;// 释放ED资源
						this.waitICUCasualtyList.remove(this.waitICUCasualtyList.size()-1);
					}else{
						if (this.ICU_Avaible_Bed_Count > 0) {// 将ICU等候列表中的人员，转移到icu列表中
							this.ICUCasualtyList = getCasualty(
									this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1),
									this.ICUCasualtyList);
							this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).enterICUTime = currentTime;// 记录伤员进入ICU的时间
							this.ICU_Avaible_Bed_Count--;// ICU资源占用
							this.ED_Available_Count++;// 释放ED资源
							// 输出伤员信息
							String s = "Hospital " + this.hid + " Casualty "
									+ this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyID
									+ " enter ICU at" + currentTime;// 输出伤员位置
							System.out.println(s);
							//

							this.waitICUCasualtyList.remove(this.waitICUCasualtyList.size()-1);// 清除waitICUCasualtyList，中的该伤员
						} else {
							xx++;// 表示在循环过程中 ICU已满，waitICU中还有xx个伤员
							// break;//之后可以考虑在这里增加每一个等待伤员的等待时间；

						}
						
						
					}
					
					
				} while (this.waitICUCasualtyList.size() > xx);
			}
		}
		//ICU入口,icu内部完成操作的
		if (this.ICUCasualtyList != null) {
			if (this.ICUCasualtyList.size() > 0) {
				int rr = 0;
				do {
					if (this.ICUCasualtyList.get(rr).RPM < 5) {
						// 表示伤员在ICU中接受处置，先停止其伤情恶化，然后根据其处置时间，到达平均处置时间后，改变其RPM，使其可以被转移到GW
						this.ICUCasualtyList.get(rr).stopDeterioration = true;// 停止伤情恶化
						double ICUtreatTimeDuration = currentTime
								- this.ICUCasualtyList.get(rr).enterICUTime;// 记录伤员在ICU中处置的时间
						Random r = new Random();
						double randomTime = r.nextDouble()*10+10; // 可以根据不同医院情况设定，当然目前先采用随机数 10~20
										// 还可以采用，经过一个随机时间之后，给伤员RPM+1的方式，改变其伤情
						if (ICUtreatTimeDuration > randomTime) {
							this.ICUCasualtyList.get(rr).RPM += 1;
						} else {// 伤员继续在ICU接受治疗
//							String s = "Hospital " + this.hid + " Casualty "
//									+ this.ICUCasualtyList.get(rr).casualtyID
//									+ " treated at ICU for " + ICUtreatTimeDuration;// 输出伤员位置
//							System.out.println(s);
							rr++;
						}
						
					} else {
						// 表明伤员可以从ICU转入GW普通病房
						this.ICUCasualtyList.get(rr).overICUTime= currentTime;// 记录伤员ICU治疗时间
						if (this.GW_Avaible_Bed_Count > 0) {// 表明GW有床位，可以将伤员从ICU转入GW
							this.GWCasualtyList = getCasualty(
									this.ICUCasualtyList.get(rr),
									this.GWCasualtyList);
							this.GW_Avaible_Bed_Count--;// 占用GW资源
							this.ICUCasualtyList.get(rr).leaveICUTime = currentTime;// 记录伤员离开ICU时间
							this.ICUCasualtyList.get(rr).enterGWTime = currentTime;// 记录伤员进入GW时间
							this.ICU_Avaible_Bed_Count++;// 释放icu资源
							// 输出伤员位置信息

							String s = "Hospital " + this.hid + " Casualty "
									+ this.ICUCasualtyList.get(rr).casualtyID
									+ " Enter the GW from ICU at" + currentTime;// 输出伤员位置
							System.out.println(s);

							this.ICUCasualtyList.remove(rr);// 将该伤员从icuCasualtyList中移除
						} else {// GW床位不足，将伤员安排在等待GW列表中
							this.waitGWCasualtyList = getCasualty(
									this.ICUCasualtyList.get(rr),
									this.waitGWCasualtyList);

//							String s = "Wait:Hospital " + this.hid
//									+ " Casualty "
//									+ this.ICUCasualtyList.get(rr).casualtyID
//									+ " wait the GW at" + currentTime;// 输出伤员位置
//							System.out.println(s);

							this.ICUCasualtyList.remove(rr);
							// this.ICU_Avaible_Bed_Count++;//释放icu资源
						}
					}
				} while (this.ICUCasualtyList.size() > rr);
			}
		}
		// GW入口
		if (this.waitGWCasualtyList != null) {
			if (this.waitGWCasualtyList.size() > 0) {
				int ee = 0;
				do {
					if (this.GW_Avaible_Bed_Count > 0) {// 表明有可用床位。将waitGWList中的伤员转移到GWlist中来
						// 需要根据伤员使用ICU来的还是从ED直接来的分开处理啊
						if (this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).enterICUTime == 0.0) {// 如果waitGWList中，伤员进入ICU时间为0.0，表示其是从ED直接分流过来的
							this.GWCasualtyList = getCasualty(
									this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1),
									this.GWCasualtyList);// 先转移伤员
							this.GW_Avaible_Bed_Count--;// 占用GW资源
							this.ED_Available_Count++;// 释放ED资源

							// 输出伤员位置信息
							String s = "Hospital "
									+ this.hid
									+ " Casualty "
									+ this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).casualtyID
									+ " enter the GW at" + currentTime;// 输出伤员位置
							System.out.println(s);

							this.waitGWCasualtyList.remove(this.waitGWCasualtyList.size()-1);// 从wait列表中释放该伤员；
						} else {
							this.GWCasualtyList = getCasualty(
									this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1),
									this.GWCasualtyList);// 先转移伤员
							this.GW_Avaible_Bed_Count--;// 占用GW资源
							this.ICU_Avaible_Bed_Count++;// 释放占用ICU资源

							// 输出伤员位置信息
							String s = "Hospital "
									+ this.hid
									+ " Casualty "
									+ this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).casualtyID
									+ " enter the GW at" + currentTime;// 输出伤员位置
							System.out.println(s);

							this.waitGWCasualtyList.remove(this.waitGWCasualtyList.size()-1);// 从wait列表中释放该伤员；
						}

					} else {// 表明没有床位
						ee++;// 表示循环中，GW不够，将ee个伤员留在waitGWList中
						// break;//跳出循环
					}

				} while (this.waitGWCasualtyList.size() > ee);
			}

		}
		if (this.GWCasualtyList != null) {
			if (this.GWCasualtyList.size() > 0) {
				int tt = 0;
				do {
					if (this.GWCasualtyList.get(tt).RPM == 12) {
						// 伤员康复可以出院
						this.GWCasualtyList.get(tt).dischargeTime = currentTime;// 记录出院时间
						this.GWCasualtyList.get(tt).overGWTime = currentTime;// 记录GW治疗时间
						this.GWCasualtyList.get(tt).casualtyPositionFlag = Constants.POSITION_DISCHARGE;// 改变伤员位置状态
																										// 为出院
						String s = "Discharge:Hospital " + this.hid
								+ " Casualty "
								+ this.GWCasualtyList.get(tt).casualtyID
								+ " discharge at" + currentTime;// 输出伤员位置
						System.out.println(s);

						this.GWCasualtyList.remove(tt);// 释放该伤员
						this.GW_Avaible_Bed_Count++;// 释放GW资源
					} else {
						// 表示伤员在GW接受处置
						this.GWCasualtyList.get(tt).stopDeterioration = true;// 停止伤情恶化
						double GWTreatTimeDuration = currentTime
								- this.GWCasualtyList.get(tt).enterGWTime;// 在GW中的处置时间，
						Random r = new Random();
						double randomTime = r.nextDouble()*2+8;// 8~10
						if (GWTreatTimeDuration > randomTime) {
							this.GWCasualtyList.get(tt).RPM += 1;
						} else {// 伤员
//							String s = "Hospital " + this.hid
//									+ " Casualty "
//									+ this.GWCasualtyList.get(tt).casualtyID
//									+ " treat at GW for" + GWTreatTimeDuration;// 输出伤员位置
//							System.out.println(s);
							tt++;
						}
						
					}
				} while (this.GWCasualtyList.size() > tt);
			}
		}

	}

	// 获取伤员列表，被ED或WAITED 调用
	private List<Casualty> getCasualty(Casualty cc, List<Casualty> casualtyList) {
		List<Casualty> tempCasualtyList = new ArrayList<Casualty>();
		for (int a=0; a<MCIContextBuilder.casualtyList.size(); ++a) {			
			Casualty mm = MCIContextBuilder.casualtyList.get(a);
				if (mm.casualtyID == cc.casualtyID) {
					tempCasualtyList.add(mm);
				}			
		}
		if (casualtyList != null) {
			tempCasualtyList.addAll(casualtyList);
		}
		return tempCasualtyList;
	}

	public String toString() {
		return String.format("HabitatCell @ location (%d, %d)", x, y);
	}

}
