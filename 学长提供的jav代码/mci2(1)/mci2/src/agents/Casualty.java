package agents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import common.Constants;
import main.MCIContextBuilder;

public class Casualty {
	/*伤员Agent
	 * 
	 * 事件发生地点   伤员初始位置
	 * 伤员唯一编号
	 * InitalRPM 伤员初始伤情，RPM值0——12 之间的数字
	 * 伤员分流标识,red（RPM 1-4）,yellow(5-8),green(9-12),black(0)  //分流标识有 现场分流agent完成，分流伤员数量由分流人员数量决定，单位分流人员，单位时间内的分流决策人数是一定的
	 * 分流标识产生的时刻
	 * 
	 * 伤员搭载急救车编号          /急救车到达急救现场后，第一辆急救车的人员扮演分流人员，后续的到达的急救车完成后送，
	 *                   或者急救车agent就是完成后送任务，由分流agent完成分流活动，而不去关分流人员如何到达现场的，建议采用后者
	 * 伤员搭载急救车时刻         /急救车到达现场后，按照装载规则产生的结果，将伤员中的搭载急救车编号赋予急救车唯一号，
	 *                   而急救车中的伤员列表中添加响应的伤员编号，当搭载决策做出之后伤员搭载急救车时刻属性记录该时刻
	 * 
	 * 伤员到达后送医院时刻      //当急救车将伤员后送到预期的医院后，先判断ED unit的数量是否饱和，如果饱和伤员需要等待
	 * 伤员进入ED时刻               //如果ED unit数量没有饱和则伤员进入ED，并记录伤员进入ED的时刻，
	 *                    进入ED之后，ED unit数量-1，然后根据伤员RPM值判断，
	 *                    RPM<=4，说明伤员需要紧急手术，
	 *                           此时需要判断ICU/general ward bed count 是否饱和，如果饱和病人需要转院
	 *                                                                       如果未饱和，判断Operation_count是否饱和 饱和的话转院
	 *                                                                                                        未饱和的话伤员进入 Operation
	 *伤员进入ICU的时刻               4<RPM<=8 说明伤员伤情可以延迟处理
	 *                           此时需要判断ICU/general ward bed count 是否饱和，如果饱和病人 进入等待队列
	 *                                                                         未饱和，病人进入ICU/GW
	 *                     RPM>8  说明伤员伤势较轻，经过处理之后，伤员出院
	 *伤员进入GW的时刻                     
	 *                                                    
	 *                 
	 * 
	 * 
	 * 
	 * */
	public int casualtyID; //伤员唯一性标识
	public String casualtyPositionFlag; //伤员位置标识，初始化时位置为 Incident，搭载上医院之后为 On the way,到达医院之后为 hospital
	public int RPM; //伤员伤情，初始化时按照参数生成
	public String triageTag;//伤员分流标识，red,yellow,black,green
	public int ambulanceID;//伤员搭载的急救车ID
	
	public double triageTime;//分流时间
	public double loadAmbulanceTime;//被急救车搭载时间
	public double arrHospitalTime;//到达医院时间，
	
	public double enterEDTime; //伤员进入ED的时间
	public double enterICUTime;//伤员进入ICU时间
	public double leaveICUTime;//伤员离开ICU时间
	public double enterGWTime; //伤员进入GW的时间
	
	public double overICUTime;//伤员ICU治疗完毕时间
	public double overGWTime;//伤员GW治疗完毕时间
	public double overEDTime;//伤员ED治疗完毕时间
	public double leaveEDTime;//伤员ED治疗完毕时间
	
	public double dischargeTime;//伤员出院时间
	
	public double expectSurTime;//伤员预期的生存时间
	//static ISchedule schedule;//时间记录需要
	
	public String deadPosition;//记录伤员死亡地点
	public double deadTime;//记录伤员死亡时间
	public boolean valuableOfRevise = false;//伤员有价值转运（不会途中挂掉）
	public boolean used = false; //转运临时使用

	
	public Casualty(int id,int rpm){
		this.casualtyID=id; //初始化赋值，伤员ID
		casualtyPositionFlag=Constants.POSITION_INCIDENT; //初始位置
		this.InitialRPM=rpm; //初始化赋值
		this.RPM=rpm; //初始化赋值，伤员伤情RPM，满足特定分布，先按正太分布进行
		this.triageTag=null;
		this.stopDeterioration=false;//初始条件下，标识伤员伤情不停恶化
	//	this.expectSurTime=suvivalTimeCount(rpm);//根据初始rpm计算伤员预期的生存时间。
		
	}
	public void step(){

		double currentTime = MCIContextBuilder.currentTime;
		//System.out.println("currentTime "+currentTime);
		if(this.RPM<=0){
			this.stopDeterioration=true;
//			String out="Casualty "+this.casualtyID+" dead "+"at "+currentTime+" at "+this.casualtyPositionFlag;
//			System.out.println(out);
			if(this.deadTime ==0.0){ //对于还没有记录死亡信息的伤员，记录死亡地点，和死亡时间，对于已经记录过的，无需再次记录
				String out="Casualty "+this.casualtyID+" dead "+"at "+currentTime+" at "+this.casualtyPositionFlag;
				System.out.println(out);
				this.deadPosition=this.casualtyPositionFlag;//病人死亡时，先记录其死亡位置
				this.deadTime=currentTime;//记录数伤员死亡时间
				this.casualtyPositionFlag=Constants.POSITION_DEAD; //如果伤员死亡将其位置赋值为dead
			}
			
			//die();
		}else{
			
			//伤员RPM减少，依据初始值，减少比例不同
			if(!this.stopDeterioration){//恶化标识为false,标识伤员伤情一直恶化
				this.RPM=getCurrentRPM(currentTime,this.InitialRPM);
				//System.out.println("currentrpm "+this.RPM);
			}else
			{//伤情恶化停止,不对RPM进行修改
				
			}
		
		}
	}
	
//	private void die(){
//		
//		ContextUtils.getContext(this).remove(this);
//		
//	}
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

}
