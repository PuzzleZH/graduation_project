package agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import common.Constants;
import main.MCIContextBuilder;

public class Ambulance {
		
	private static int uniqueID=0; 
	public int ambulanceID; //?????��????
	
	public String positionFlag; // ???????��????  "Base"??????????????"Incident" ?????????????????"Hospital"??????????????
	
	private int x,y;                    // ?????????��??
	
	public Hospital target_hospital;       //??????????
	private Incident inc;               //??????
	public int targetx;       //??????????? ????
	public int targety;
	//private String Heading;       //???????????
	private boolean triageStart; //????????????????????????????????????????????????????
	
	public List<Casualty> casualtyList;//?????????????��?
	//static ISchedule schedule;//????????
	
	/**??????????**/
	public Ambulance(int x,int y,int aid){
		if(x<0){
			throw new IllegalArgumentException(String.format("Coordinate x=%d<0.",x));
		}
		if(y<0){
			throw new IllegalArgumentException(String.format("Coordinate y=%d<0.",y));
		}
		this.x=x;
		this.y=y;              //?????????��??
		this.ambulanceID=aid;    //???????ID  ???????0 ??????
		this.positionFlag=Constants.POSITION_BASE; //????????��?????څ
		this.target_hospital=null;  //??????????????
		this.casualtyList=null;
		this.triageStart=false;//???????????��??????????????????????????
	}

	
	
	
	

	//?????????
	public void  step(){
		

		double currentTime=MCIContextBuilder.currentTime;		
		

		if(positionFlag==Constants.POSITION_BASE){//???????????????????????????????څ?Incident??????????
			
			this.targetx=MCIContextBuilder.inc.x; //????????��???? ???? ??????
			this.targety=MCIContextBuilder.inc.y;
			System.out.println(this.ambulanceID+"currenttarget "+this.targetx);
			positionFlag=Constants.POSITION_ONTHEWAY;//???????????? ??��??
			
		} 
		if(positionFlag==Constants.POSITION_INCIDENT)
		{
			//System.out.println(this.ambulanceID+" arrive incident");
//			    
//				//setTriageTag();
//				//??????????????
//				this.casualtyList=loadCasualty(Constants.LOADCASUALTY_MODE);
//				if(this.casualtyList.size()>0){
//					//????????
//					if(Constants.CHOOSE_HOSPITAL_MODE==Constants.CHOOSE_HOSPITAL_MODE_3){
//						this.target_hospital=chooseHospitalBasedOnRPM(this.casualtyList);
//					}else{
//						this.target_hospital=chooseHospital(Constants.CHOOSE_HOSPITAL_MODE);
//						
//					}
//					
//					
//					
//					//getTargetHospital(Casualty.ID),???????????hospital;
//					//target_hospital=getTargetHospital(this.id);
//					//???????????????????????
//					//target=getSpace.getLocation(target_hospital);
//					String out="Ambulance "+this.ambulanceID+" travel to"+" Hospital "+this.target_hospital.hid;
//					for(int jj=0;jj<this.casualtyList.size();++jj){
//						out+=" Casualty "+this.casualtyList.get(jj).casualtyID+" ";
//						this.casualtyList.get(jj).casualtyPositionFlag=Constants.POSITION_ONTHEWAY;//??????��????????��??
//						this.casualtyList.get(jj).loadAmbulanceTime=currentTime;//???????????????????????
//						this.casualtyList.get(jj).ambulanceID=this.ambulanceID;//??????????????????
//					}
//					System.out.println(out);
//					target=getSpace().getLocation(target_hospital);
//					positionFlag=Constants.POSITION_ONTHEWAY;//???????????? ??��??	???????????
//					
//				}else{
//					positionFlag=Constants.POSITION_INCIDENT;//??????????��????????????????????
//					System.out.println("Ambulance "+this.ambulanceID+" wait at Incident");
//				}
//
//			
			
		}
		if(positionFlag==Constants.POSITION_HOSPITAL){
			//?��????????????????????????RPM??????ICU??��??????
			//??rpm<2,????icu count<0 ??????????????????????????????????
			//????drop???????
			Hospital t=this.target_hospital;
			
			//?????????????????????????????��???
			//t.hospitalCasualtyList=getHospitalCasualtyList(this);
			if(this.casualtyList !=null){
				if(this.casualtyList.size()>0){
					int jj=0;
					do{
						Casualty oneCasualty=this.casualtyList.get(jj);
						
						if(oneCasualty.RPM>0){//??????????????????????
//							if((oneCasualty.RPM<2)&&(t.ICU_Avaible_Bed_Count<1)){//?????RPM<2 ????ICU<=0 ????????????????????????????????????????????????	
//								//???????????????????????????
//								//?څ????inc
//								this.target_hospital=chooseDiversionHospital(t.hid);//??????????????????
//								target=getSpace().getLocation(target_hospital); //????????��???? ???? ???????
//								positionFlag=Constants.POSITION_ONTHEWAY; //???????????? ??��??
//													
//								String outLine="Diversion: Ambulance "+this.ambulanceID+" diversion at "+currentTime;
//								System.out.println(outLine);
//								break;
//								
//							}else{//?????????????????????
								t.hospitalCasualtyList=getCasualty(this.casualtyList.get(jj),t.hospitalCasualtyList);	///?????????????��???
								this.casualtyList.get(jj).casualtyPositionFlag=Constants.POSITION_HOSPITAL;//??????��??????????
								this.casualtyList.get(jj).arrHospitalID=t.hid; //???????????????ID
								this.casualtyList.get(jj).arrHospitalTime=currentTime;//???????? ???????????
								
								//?څ????inc
								
								this.targetx=MCIContextBuilder.inc.x; //????????��???? ???? ??????
								this.targety=MCIContextBuilder.inc.y;
								positionFlag=Constants.POSITION_ONTHEWAY; //???????????? ??��??	
								
								//????????????��??????
								this.casualtyList.remove(jj);
//							}							
						}
						else{ //???RPM=0 ??????????????????
							 //this.casualtyList.remove(jj);
							 String outLine="Dead: Casualty "+this.casualtyList.get(jj).casualtyID+" dead on the way at "+currentTime;
							 System.out.println(outLine);
							 
							//?څ????inc
							
							this.targetx=MCIContextBuilder.inc.x; //????????��???? ???? ??????
							this.targety=MCIContextBuilder.inc.y; 
							positionFlag=Constants.POSITION_ONTHEWAY; //???????????? ??��??	
								
							//????????????��??????
							this.casualtyList.remove(jj);
							 
						}
					}while(this.casualtyList.size()>jj);
					
				}			
			}
			
			if(t.hospitalCasualtyList!=null){			
				if(t.hospitalCasualtyList.size()>Constants.AMBULANCE_CARRY_MAX){//???��????? ???????????????????????????????????????????????,3????4??????????????????????????????
					String outline="Ambulance "+this.ambulanceID+" arrived "+"Hospital "+t.hid+" drop ";
					for(int mm=0;mm<t.hospitalCasualtyList.size();++mm){
						if(t.hospitalCasualtyList.get(mm).ambulanceID==this.ambulanceID){//?????????ID??????????��??��???????
							 outline+=" Casualty "+t.hospitalCasualtyList.get(mm).casualtyID;
						}		
					}
					System.out.println(outline+" at "+currentTime);
				}
				//????????????????��??��????
				if((t.hospitalCasualtyList.size()>0)&&(t.hospitalCasualtyList.size()<Constants.AMBULANCE_CARRY_MAX+1)){
					String out="Ambulance "+this.ambulanceID+" arrived "+"Hospital "+t.hid+" drop ";
					for(int mm=0;mm<t.hospitalCasualtyList.size();++mm){
					    out+=" Casualty "+t.hospitalCasualtyList.get(mm).casualtyID;	
					}
					System.out.println(out+" at "+currentTime);
				}
			}
			/*
			for(int jj=0;jj<this.casualtyList.size();++jj){
				this.casualtyList.remove(jj);
			}*/	
		}
		if(positionFlag==Constants.POSITION_ONTHEWAY){  //????????????? ??��?? ??????????????????????
			for(int i = 1;i<=Constants.AMBULANCE_TRAVEL_SPEED;i++) {
			moveTowards(this.targetx,this.targety);
			}
		}
	
     }
	
	
	/**??????????????????????????????1??**/
	private void moveTowards(int x, int y){
		int disx = Math.abs(x-this.x);
		int disy = Math.abs(y-this.y);
		if(disx>0 && disy>0){
			double diff = (double)disx/((double)disx+(double)disy);
			Random r = new Random();
			double d1 = r.nextDouble();

			if(d1<diff) {
				this.x = moveAbit(this.x, x);
			}else {
				this.y = moveAbit(this.y, y);
			}
		
		}else if(disx>0) {
			this.x = moveAbit(this.x, x);
		}else if(disy>0) {
			this.y = moveAbit(this.y, y);
		}
		else{
			if (this.x == MCIContextBuilder.inc.x && this.y == MCIContextBuilder.inc.y){//????????????????????
				this.positionFlag=Constants.POSITION_INCIDENT;//????????????  ??????��????????????					
			}else{
				this.positionFlag=Constants.POSITION_HOSPITAL;//????????????????��??????????
			}	
			
		}				
	}
	private int moveAbit(int x,int y) {
		if(x>y) {
			x-=1;
		}else if(x<y) {
			x+=1;
		}
		return x;
	}

	
//	//????????��???????
//		private void setTriageTag(){
//	        //????????casualty???????????????tag???????????RPM????tag???????????Constants.TRIAGE_NUM_AT_ONE_TIME???????????��?????		
//			List<Object> casualtiesIncidentNotTag=new ArrayList<Object>();
//			for(Object obj:getGrid().getObjects()){
//				if(obj instanceof Casualty){
//					Casualty dd=(Casualty)obj;
//					if((dd.casualtyPositionFlag==Constants.POSITION_INCIDENT)&&(dd.triageTag==null)){
//						casualtiesIncidentNotTag.add(obj);//??????????????tag??casualty????
//					}
//					
//				}
//			}
//			
//			if(casualtiesIncidentNotTag.size()>0){
//				int temp=0;
//				do{
//					int index=RandomHelper.nextIntFromTo(0, casualtiesIncidentNotTag.size()-1);//??Casualties?????????????
//					Object obj=casualtiesIncidentNotTag.get(index);
//					Casualty aa=(Casualty)obj;  //??????????Casualty????
//					double casualtyIniRPM=aa.RPM;
//					temp++;
//					String tag=null;
//					if(casualtyIniRPM==0){ //?????RPM????0????????????????
//						tag=Constants.TAG_BLACK;
//					}else if((casualtyIniRPM>0)&&(casualtyIniRPM<5)){  //???RPM??1-4 ???,????red
//						tag=Constants.TAG_RED;					
//					}else if((casualtyIniRPM>4)&&(casualtyIniRPM<9)){ //???RPM ??5-8???????yellow
//						tag=Constants.TAG_YELLOW;
//					}else{
//						tag=Constants.TAG_GREEN;
//					}
//					aa.triageTag=tag;//??????????????casualty????
//					System.out.println("Casualty "+aa.casualtyID+" tag "+aa.triageTag.toString());//???????????????????��????
//				}while(temp<Constants.TRIAGE_NUM_AT_ONE_TIME);
//				
//			}
//			
//		}
//	
//	
//	/*??????????????*/
//	private Hospital getTargetHospital(int AmbulanceId){
//		
//		Hospital temp=target_hospital;
//		for(Object obj:getGrid().getObjects()){
//			if(obj instanceof Hospital){
//				temp=(Hospital)obj;
//				if(temp.hid==AmbulanceId){
//					target_hospital=temp;
//					break;
//				}
//			}
//		}
//		return target_hospital;
//	}
//	/**???????????????*/
//	private List<Casualty> loadCasualty(int loadPatientFlag){
//		//????????????????��?????????????????????????????��?
//		//??????????tag?????
//		//List<Object> casualtiesHaveTag=new ArrayList<Object>();
//		List<Casualty> tempCasualtyList=new ArrayList<Casualty>();
//		
//		List<Object> casualtiesTagRed=new ArrayList<Object>();
//		List<Object> casualtiesTagYellow=new ArrayList<Object>();
//		List<Object> casualtiesTagGreen=new ArrayList<Object>();
//		for(Object obj:getGrid().getObjects()){ //?????????????????????????????????��???
//			if(obj instanceof Casualty){
//				Casualty mm=(Casualty)obj;
//				if(mm.casualtyPositionFlag==Constants.POSITION_INCIDENT){
//					if(mm.triageTag==Constants.TAG_RED){
//						casualtiesTagRed.add(obj);
//					}
//					if(mm.triageTag==Constants.TAG_YELLOW){
//						casualtiesTagYellow.add(obj);
//					}
//					if(mm.triageTag==Constants.TAG_GREEN){
//						casualtiesTagGreen.add(obj);
//					}
//				}
//				
//		
//			}
//		}
//		int temp=0;
//		do{
//			if(casualtiesTagRed.size()>0){
//				//????????????????????????
//				int index=0;
//				if(loadPatientFlag==Constants.LOADCASUALTY_MODE_0){
//					index=RandomHelper.nextIntFromTo(0, casualtiesTagRed.size()-1);//??CasualtiesRed??????????????????????????????????��????????????????????????
//				}
//				else if(loadPatientFlag==Constants.LOADCASUALTY_MODE_1){
//					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagRed);// ??RPM??��?? ???? ???????
//				}else{
//					index=chooseCasualtyIndexBasedOnRPM_lightfirst(casualtiesTagRed);// ??RPM???? ???? ???????
//				}
//				
//				
//				Object obj=casualtiesTagRed.get(index);
//				Casualty ss=(Casualty)obj;
//				tempCasualtyList.add(ss);
//				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //??????????,????��???????????��??
//				casualtiesTagRed.remove(index); //????????????????????��????????
//				temp++;
//			}else if(casualtiesTagYellow.size()>0){
//				int index=0;
//				if(loadPatientFlag==Constants.LOADCASUALTY_MODE_0){
//					index=RandomHelper.nextIntFromTo(0, casualtiesTagYellow.size()-1);//??CasualtiesYellow?????????????
//				}
//				else if(loadPatientFlag==Constants.LOADCASUALTY_MODE_1){
//					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagYellow);// ??RPM??��?? ???? ???????
//				}else{
//					index=chooseCasualtyIndexBasedOnRPM_lightfirst(casualtiesTagYellow);// ??RPM???? ???? ???????
//				}
//				Object obj=casualtiesTagYellow.get(index);
//				Casualty ss=(Casualty)obj;
//				tempCasualtyList.add(ss);
//				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //??????????,????��???????????��??
//				casualtiesTagYellow.remove(index); //????????????????????��????????
//				temp++;
//			}else if(casualtiesTagGreen.size()>0){
//				int index=0;
//				if(loadPatientFlag==Constants.LOADCASUALTY_MODE_0){
//					index=RandomHelper.nextIntFromTo(0, casualtiesTagGreen.size()-1);//??CasualtiesGreen?????????????
//				}
//				else if(loadPatientFlag==Constants.LOADCASUALTY_MODE_1){
//					index=newChooseCasualtyIndexBasedOnRPM(casualtiesTagGreen);// ??RPM??��?? ???? ???????
//				}else{
//					index=chooseCasualtyIndexBasedOnRPM_lightfirst(casualtiesTagGreen);// ??RPM????? ???? ???????
//				}
//				Object obj=casualtiesTagGreen.get(index);
//				Casualty ss=(Casualty)obj;
//				tempCasualtyList.add(ss);
//				ss.casualtyPositionFlag=Constants.POSITION_ONTHEWAY; //??????????,????��???????????��??
//				casualtiesTagGreen.remove(index); //?????????????????????��????????
//				temp++;
//			}else{
//				temp++;// ?????��????????????
//			}	
//			
//		}while(temp<Constants.AMBULANCE_CARRY_MAX);
//		
//		return tempCasualtyList;
//		
//	}
//	//????????????��??? ????????RPM??��???????????????,??????????????????????????,
//	private int chooseCasualtyIndexBasedOnRPM(List<Object> casualtyList){
//		int index=0;//??????????
//		int minRPM=12;//??��RPM?????
//		for(int i=0;i<casualtyList.size();++i){
//			Casualty one=(Casualty)casualtyList.get(i);//??obj??????Casualty????
//			if(one.RPM<minRPM){//???????????RPM��????��RPM??????????????minRPM
//				minRPM=one.RPM;
//				index=i;//??????????????????? index ???????
//			}
//		}
//		return index;
//	}
//	
//	private int newChooseCasualtyIndexBasedOnRPM(List<Object> casualtyList){
//		int index=0;//??????????
//		int minRPM=12;//??��RPM?????
//		//?????????
//		List<Object> hospitalList=new ArrayList<Object>();
//		
//		for(Object obj:getSpace().getObjects()){
//			if(obj instanceof Hospital){
//				hospitalList.add(obj);
//			}
//		}			
//				
//		//??? ???????????��??��???????????????? ????rpm
//		for(int i=0;i<casualtyList.size();++i){
//			Casualty one=(Casualty)casualtyList.get(i);//??obj??????Casualty????
//			one.valuableOfRevise = false;
//			int[] expectRPM=new int[Constants.HOSPITAL_COUNT]; //??????? expectRPM[??amb?��???????????????casualtyID][??ID]
//			for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
//				Hospital oneHospital=(Hospital)hospitalList.get(j);//??????????????? ??????
//				expectRPM[oneHospital.hid]=newExpectRPM(one,oneHospital);//???? ????RPM
//				if(expectRPM[oneHospital.hid]>0) {
//					one.valuableOfRevise = true;
//					break;
//				}
//			}
//			if((one.RPM<minRPM) &&(one.valuableOfRevise = true)){//???????????RPM��????��RPM??????????????minRPM  ??????????????
//				minRPM=one.RPM;
//				index=i;//??????????????????? index ???????
//			}
//		}
//		return index;
//	}
//	
//	//????????????��??? ????????RPM?????????????????,??????????????????????,
//		private int chooseCasualtyIndexBasedOnRPM_lightfirst(List<Object> casualtyList){
//			int index=0;//??????????
//			int MaxRPM=0;//??��RPM?????
//			for(int i=0;i<casualtyList.size();++i){
//				Casualty one=(Casualty)casualtyList.get(i);//??obj??????Casualty????
//				if(one.RPM>MaxRPM){//???????????RPM��????��RPM??????????????minRPM
//					MaxRPM=one.RPM;
//					index=i;//??????????????????? index ???????
//				}
//			}
//			return index;
//		}
//	
//	/**???????????????????????*/
//	//??????????????????????????????????
//		private Hospital chooseHospital(int chooseMode){
//			//?????????
//			List<Object> hospitalList=new ArrayList<Object>();	
//			
//			
//			int index=0;
//			int hospitalUsedCountList[]=new int[Constants.HOSPITAL_COUNT];//????????��? 
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					hospitalList.add(obj);
//					Hospital tt=(Hospital)obj;
//					//???????????? ??????��???????
//					int usedCount=(Constants.ED_AVAILABLE_COUNT+Constants.ICU_AVAILABLE_COUNT+Constants.GW_AVAILABLE_COUNT)-(tt.ED_Available_Count+tt.ICU_Avaible_Bed_Count+tt.GW_Avaible_Bed_Count);
//					//????? ???����???? ???��???? ????????????
//					hospitalUsedCountList[tt.hid]=usedCount;
//				}
//			}
//			
//			if(chooseMode==Constants.CHOOSE_HOSPITAL_MODE_0){ //????????
//				//??????????????????????
//				index=RandomHelper.nextIntFromTo(0, hospitalList.size()-1);
//				Hospital tHospital=(Hospital)hospitalList.get(index);	
//				return tHospital;
//			}else if(chooseMode==Constants.CHOOSE_HOSPITAL_MODE_1) {//????count??��??
//				//????????��???��?????????��???i ?????index
//				int min=hospitalUsedCountList[0];
//				for(int i=0;i<hospitalUsedCountList.length;++i){
//					if(min>hospitalUsedCountList[i]){
//						min=hospitalUsedCountList[i];
//						index=i;			
//					}
//				}
//				
//				//??????hid?index?????????index?????????hid????
//				
//				List<Object> hospitalResult=new ArrayList<Object>();
//				for(Object obj:getSpace().getObjects()){
//					if(obj instanceof Hospital){
//						Hospital oneHospital=(Hospital)obj;
//						if(oneHospital.hid==index){
//							hospitalResult.add(obj);
//							
//						}
//					}
//				}	
//				Hospital tHospital=(Hospital)hospitalResult.get(0);	
//				return tHospital;
//			}else{//?????????ED????????????????????
//				double[][] avgWaitTime=getAvgWaitTime();//??????????????????????????????
//				double minAvgWaitTime=avgWaitTime[0][Constants.DEPT_ED]+avgWaitTime[0][Constants.DEPT_ICU]+avgWaitTime[0][Constants.DEPT_GW];
//				for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
//					if(minAvgWaitTime>(avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW])){
//						index=i;//????��?? ??ID?????index
//						minAvgWaitTime=avgWaitTime[i][Constants.DEPT_ED]+avgWaitTime[i][Constants.DEPT_ICU]+avgWaitTime[i][Constants.DEPT_GW];
//					}
//				}
//				
//	            //??????hid?index?????????index?????????hid????
//				List<Object> hospitalResult=new ArrayList<Object>();
//				for(Object obj:getSpace().getObjects()){
//					if(obj instanceof Hospital){
//						Hospital oneHospital=(Hospital)obj;
//						if(oneHospital.hid==index){
//							hospitalResult.add(obj);
//							
//						}
//					}
//				}	
//				Hospital tHospital=(Hospital)hospitalResult.get(0);	
//				return tHospital;
//				
//			}
//			//???��???????count??????????????????
//			
//			
//		}
//		//???????????????�????????????????????????????
//		private double[][] getAvgWaitTime(){
//			double[][] waitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//???????????????????????????
//			int[][]    treatedCasualty=new int[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT];//???????????????????????????????
//			double[][] avgWaitTime=new double[Constants.HOSPITAL_COUNT][Constants.DEPT_COUNT]; //?????????
//			// ??????????????????
//			for (Object obj : getGrid().getObjects()) {
//				if (obj instanceof Casualty) {
//					Casualty aCasualty = (Casualty) obj;
//					//if (aCasualty.casualtyPositionFlag == Constants.POSITION_HOSPITAL) {// ???????????????????  ??????????????????????????????????????????????????????
//					if (aCasualty.arrHospitalTime!=0.0) {// ?????????????0??????????????????????????????????��???????????????????????????????????????????????????????????
//								
//						double waitEDTime = 0.0; // ?????0.0
//						double waitICUTime = 0.0; // ?????0.0
//						double waitGWTime = 0.0; // ?????0.0
//						if (aCasualty.enterEDTime != 0.0) {// ??????????ED?????enterEDTime
//															// ?0.0
//															// ???????????????��?????????????ED??????
//							waitEDTime = aCasualty.enterEDTime
//									- (aCasualty.arrHospitalTime);// ??????????ED?????
//							//??????arrHospitalID ??ED ?��???????????
//							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ED]++;
//						}
//						if ((aCasualty.enterEDTime != 0.0)
//								&& (aCasualty.enterICUTime != 0.0)
//								&& ((aCasualty.enterGWTime == 0.0))) {// ??????????ICU??????��???GW
//							// ??????????ICU?????
//							waitICUTime = aCasualty.enterICUTime
//									- aCasualty.enterEDTime;
//							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
//						}
//						if ((aCasualty.enterEDTime != 0.0)
//								&& (aCasualty.enterICUTime == 0.0)
//								&& ((aCasualty.enterGWTime != 0.0))) {// ??????????GW????��???ICU
//							// ???? ??ED???????GW?????
//							waitGWTime = aCasualty.enterGWTime
//									- aCasualty.enterEDTime;
//							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;
//
//						}
//						if ((aCasualty.enterEDTime != 0.0)
//								&& (aCasualty.enterICUTime != 0.0)
//								&& ((aCasualty.enterGWTime != 0.0))) {// ???????????ICU,????????GW
//							// ???? ??ICU???????GW?????
//							waitGWTime = aCasualty.enterGWTime
//									- aCasualty.leaveICUTime;
//							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_GW]++;
//							
//							waitICUTime = aCasualty.enterICUTime
//									- aCasualty.enterEDTime;
//							treatedCasualty[aCasualty.arrHospitalID][Constants.DEPT_ICU]++;
//						}
//
//						// ?????????????????? ??????
//						waitTime[aCasualty.arrHospitalID][Constants.DEPT_ED] += waitEDTime;
//						waitTime[aCasualty.arrHospitalID][Constants.DEPT_ICU] += waitICUTime;
//						waitTime[aCasualty.arrHospitalID][Constants.DEPT_GW] += waitGWTime;
//					}
//				}
//			}
//			
//			//????????????????????????????
//			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
//				for(int j=0;j<Constants.DEPT_COUNT;++j){
//					if(treatedCasualty[i][j]==0){//???????????????0
//						avgWaitTime[i][j]=0;//??????0
//					}else{
//						avgWaitTime[i][j]=waitTime[i][j]/treatedCasualty[i][j];
//					}
//					
//				}
//			}
//			return avgWaitTime;//??????????????��?
//			
//			
//		}
//		
		// ??????????????????????????????��?,?? cc????? ?????list??
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
		
//	// ??????????,??????????????????
//		private Hospital chooseDiversionHospital(int HospitalID){
//			//?????????
//			List<Object> hospitalList=new ArrayList<Object>();	
//			int index=0;
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					Hospital tt=(Hospital)obj;
//					if(tt.hid!=HospitalID){//??????��???????????? ????????????��???
//						hospitalList.add(obj);
//					}	
//				}
//			}
//			index=RandomHelper.nextIntFromTo(0, hospitalList.size()-1);
//			Hospital tHospital=(Hospital)hospitalList.get(index);	
//			return tHospital;		
//		}
//    /**????????????????????????
//     * ????? ?????????????
//     * ???? ?????? hospital????
//     * */
//		private Hospital chooseHospitalBasedOnRPM(List<Casualty> ambCasualtyList){
//			//?????????
//			List<Object> hospitalList=new ArrayList<Object>();
//			//List<Object> ambulanceCasualtyList=new ArrayList<Object>();
//			List<Object> hospitalResult=new ArrayList<Object>();
//			
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					hospitalList.add(obj);
//				}
//			}			
//			
//			int[][] expectRPM=new int[ambCasualtyList.size()][Constants.HOSPITAL_COUNT]; //??????? expectRPM[??amb?��???????????????casualtyID][??ID]
//			//??? ???????????��??��???????????????? ????rpm
//			
//			for(int i=0;i<ambCasualtyList.size();++i){
//				for(int j=0;j<Constants.HOSPITAL_COUNT;++j){
//					Hospital oneHospital=(Hospital)hospitalList.get(j);//??????????????? ??????
//					expectRPM[i][oneHospital.hid]=newExpectRPM(ambCasualtyList.get(i),oneHospital);//???? ???amb.casualtyList.get(i)???? ????? oneHospital? ????RPM
//				}
//				
//			}
//			
//			//??????????????  ???????????????????RPM???? ????????????????????? ?????????? RPM?????????????? expectRPM??????
//			int[] sumCasualtyExpectRPM=new int[Constants.HOSPITAL_COUNT];//????????  ??????RPM???
//			for(int i=0;i<Constants.HOSPITAL_COUNT;++i){
//				sumCasualtyExpectRPM[i]=0;//?????? ???RPM??? ?????0  
//				for(int j=0;j<ambCasualtyList.size();++j){
//					sumCasualtyExpectRPM[i]+=expectRPM[j][i];// ???��??? ??????? ????? ?????????RPM???
//				}
//			}
//			int index=0;//?????????????????????????????
//			//??sumCasualtyExpectRPM?????
//			int maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[index];// ????????? ???????RPM?????? ???RPM
//			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
//				if(sumCasualtyExpectRPM[i]>maxSumCasualtyExpectRPM){
//					maxSumCasualtyExpectRPM=sumCasualtyExpectRPM[i];
//					index=i;//??rpm??????????????????
//				}
//			}
//			
//			//???sumCasualtyExpectRPM???????????????????????????????��???????????index
//			ArrayList<Integer> indexList=new ArrayList<Integer>();
//			for(int i=0;i<sumCasualtyExpectRPM.length;++i){
//				if(sumCasualtyExpectRPM[i]==maxSumCasualtyExpectRPM){
//					indexList.add(i);
//				}
//			}
//			int x=RandomHelper.nextIntFromTo(0, indexList.size()-1);
//			index=(int)indexList.get(x);//???????????index?��??????????????��???????
//			
//			//??????hid?index?????????index?????????hid????
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					Hospital oneHospital=(Hospital)obj;
//					if(oneHospital.hid==index){
//						hospitalResult.add(obj);
//						
//					}
//				}
//			}	
//			Hospital tHospital=(Hospital)hospitalResult.get(0);	
//			return tHospital;
//			
//		}
//		
//		/**??????????
//		 * ????? Casualty ????? Hospital????
//		 * ????  Casualty????Hospital ?????????????RPM???
//		 * */
//		private int ExpectRPM(Casualty casualty, Hospital hospital){
//			//??casualty????
//		//	List<Object> localCasualty=new ArrayList<Object>(); //???????????????��???
//			List<Object> localHospital=new ArrayList<Object>();// ??????????????��?
//			
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					Hospital oneHospital=(Hospital)obj;
//					if(oneHospital.hid==hospital.hid){
//						localHospital.add(obj);
//					}
//				}
//			}	
//			int expectRPM=0;// ??????RPM
//			double[][] allHospitalAvgWaitTime=getAvgWaitTime();//????????????��??????????????????
//			//Casualty aCasualty=(Casualty)localCasualty.get(0);//??? ????????  ?????????????????????????????
//			//Hospital aHospital=(Hospital)localHospital.get(0);//??? ????????  ????????? ??????????????
//			//??????????????????
//			NdPoint ambulanceNow=getSpace().getLocation(this);// ????????????��??
//			NdPoint hospitalPosition=getSpace().getLocation(localHospital.get(0));//?????????��??
//			double dis=getSpace().getDistance(ambulanceNow, hospitalPosition);  //???? ??????????????
//			double travelTime=dis/Constants.AMBULANCE_TRAVEL_SPEED;   //???? ???????????????
//			//???????RPM
//			expectRPM=getCurrentRPM(travelTime,casualty.InitialRPM);
//			
//			if(expectRPM<5){//??????????????????????ICU
//				//????expectRPM
//				double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
//				
//			}else if(expectRPM<9){//??????????????????????GW
//				double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
//				
//			}else{//??????????????????????ED
//				double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);		
//			}
//			
//			return expectRPM;//????
//			
//			
//		}
//		
//		private int newExpectRPM(Casualty casualty, Hospital hospital){     //???????RPM???????????????? zyh
//			//??casualty????
//		//	List<Object> localCasualty=new ArrayList<Object>(); //???????????????��???
//			List<Object> localHospital=new ArrayList<Object>();// ??????????????��?
//			
//			for(Object obj:getSpace().getObjects()){
//				if(obj instanceof Hospital){
//					Hospital oneHospital=(Hospital)obj;
//					if(oneHospital.hid==hospital.hid){
//						localHospital.add(obj);
//					}
//				}
//			}	
//			int expectRPM=0;// ??????RPM
//			schedule = RunEnvironment.getInstance().getCurrentSchedule();
//			double currentTime = schedule.getTickCount();
//			double[][] allHospitalAvgWaitTime=getAvgWaitTime();//????????????��??????????????????
//			//Casualty aCasualty=(Casualty)localCasualty.get(0);//??? ????????  ?????????????????????????????
//			//Hospital aHospital=(Hospital)localHospital.get(0);//??? ????????  ????????? ??????????????
//			//??????????????????
//			NdPoint ambulanceNow=getSpace().getLocation(this);// ????????????��??
//			NdPoint hospitalPosition=getSpace().getLocation(localHospital.get(0));//?????????��??
//			double dis=getSpace().getDistance(ambulanceNow, hospitalPosition);  //???? ??????????????
//			double travelTime=dis/Constants.AMBULANCE_TRAVEL_SPEED;   //???? ???????????????
//			//???????RPM
//			expectRPM=getCurrentRPM(currentTime-casualty.triageTime+travelTime,casualty.InitialRPM);
////			expectRPM=getCurrentRPM(travelTime,casualty.InitialRPM);
//			if(expectRPM<5){//??????????????????????ICU
//				//????expectRPM
//				double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//??????????? ???????+?????ICU???????
//				//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ICU];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
//				
//			}else if(expectRPM<9){//??????????????????????GW
//				double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//??????????? ???????+?????ICU???????
//				//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_GW];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);
//				
//			}else{//??????????????????????ED
//				double reviseTime=currentTime-casualty.triageTime+travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//??????????? ???????+?????ICU???????
//				//double reviseTime=travelTime+allHospitalAvgWaitTime[hospital.hid][Constants.DEPT_ED];//??????????? ???????+?????ICU???????
//				expectRPM=getCurrentRPM(reviseTime,casualty.InitialRPM);		
//			}
//			
//			return expectRPM;//????
//			
//			
//		}
//		
		
		/*??????????????????RPM??????????????RPM*/
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

	/**?????????????????????????????????????????????????��???*/
	/*	private List<Casualty> getHospitalCasualtyList(Ambulance amb){
			
			schedule=RunEnvironment.getInstance().getCurrentSchedule();
			double currentTime=schedule.getTickCount();
			
			List<Casualty> tempHospCasualtiesList=new ArrayList<Casualty>();
			//tempHospCasualtiesList=this.hospitalCasualtyList;
			
			Context<Object> context=ContextUtils.getContext(this);
			Iterable<Object> casualties=new ArrayList<Object>();
			casualties=context.getObjects(Casualty.class);
			//List<Object> tempHospCasualtiesList=new ArrayList<Object>();
			if(amb.casualtyList.size()>0){
				for(int jj=0;jj<amb.casualtyList.size();++jj){
					for(Object obj:casualties){
						Casualty temp=(Casualty)obj;
						if(temp.casualtyID==amb.casualtyList.get(jj).casualtyID){
							temp.casualtyPositionFlag=Constants.POSITION_HOSPITAL;
							temp.arrHospitalTime=currentTime;//?????????????????
							tempHospCasualtiesList.add(temp);
							break;
						}
					}
					
				}
			}
			if(amb.target_hospital.hospitalCasualtyList!=null){
				tempHospCasualtiesList.addAll(amb.target_hospital.hospitalCasualtyList);
			}
			return tempHospCasualtiesList; //????????????????????????????????????????????????????????��???
			
			
		} */
		
	
}
