package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import common.Constants;
import main.MCIContextBuilder;


public class Hospital {
	public int x, y; // ҽԺλ������ ,����Grid�����λ��
	public int hid; // ҽԺ��Ψһ���
	public List<Casualty> hospitalCasualtyList; // ����ҽԺ����Ա�б�
	//static ISchedule schedule;// ʱ���¼��Ҫ
 
	public int ED_Available_Count;// ҽԺED��������
	public List<Casualty> EDCasualtyList;// ҽԺEDԱ�б�
	public List<Casualty> waitEDCasualtyList;// �ȴ�ED��Ա�б�

	public int ICU_Avaible_Bed_Count;// ICU���ô�λ����
	public List<Casualty> ICUCasualtyList;// ICU��Ա�б�
	public List<Casualty> waitICUCasualtyList;// �ȴ�ICU��Ա

	public int GW_Avaible_Bed_Count;// GW���ô�λ����
	public List<Casualty> GWCasualtyList;// GW��Ա�б�
	public List<Casualty> waitGWCasualtyList;// �ȴ�GW�б�
	
	public double exp_ED_Wait_Time;  //Ԥ�ڸ����ŵȴ�ʱ��
	public double exp_ICU_Wait_Time;
	public double exp_GW_Wait_Time;
	
	public double Avg_ED_Wait_Time;  //ƽ�������ŵȴ�ʱ��
	public double Avg_ICU_Wait_Time;
	public double Avg_GW_Wait_Time;
	
	public double ED_Wait_Time;  //���������ŵȴ�ʱ��
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
		this.ED_Available_Count = Constants.ED_AVAILABLE_COUNT;// ��ʼ��ED����
		this.ICU_Avaible_Bed_Count = Constants.ICU_AVAILABLE_COUNT;// ��ʼ��ICU����
		this.GW_Avaible_Bed_Count = Constants.GW_AVAILABLE_COUNT;// ��ʼ��GW����
	}

	public int getID() {
		return this.hid;
	}


	public void step() {

		double currentTime=MCIContextBuilder.currentTime;
		
		//ED�ȴ��б��ȶ�������жϣ���֤����������еȴ���Ա�Ļ����Ȱ��������ED
		if (this.waitEDCasualtyList != null) {
			if (this.waitEDCasualtyList.size() > 0) {// �б�Ϊ�գ������еȴ�����ED����Ա
				// ����Ա��waiEDCasualtyListת�Ƶ�EDCasualtyList��
				int mm = 0;
				do {
					if(this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyPositionFlag==Constants.POSITION_DEAD){//�����Ա�Ѿ�����
						//���������Ϣ
						String s = "Dead:Hospital " + this.hid + " Casualty "
								+ this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyID
								+ " Dead waiting ED at" + currentTime;// �����Աλ��
						System.out.println(s);	
						this.waitEDCasualtyList.remove(this.waitEDCasualtyList.size()-1);//�ӵȴ��б����ͷŸ���Ա
					}else{
						if (ED_Available_Count > 0) {
							//��Ҫ�����ڵȴ�ʱ����������Ա
							EDCasualtyList = getCasualty(
									this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1),//ѡȡ�Ⱥ�����У������Ŷӵ���Ա
									this.EDCasualtyList);
							this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).enterEDTime = currentTime;// ��¼��Ա����ED��ʱ��
							ED_Available_Count--;// ռ��ED��Դ
							String s = "Hospital " + this.hid + " Casualty "
									+ this.waitEDCasualtyList.get(this.waitEDCasualtyList.size()-1).casualtyID
									+ " Enter the ED at" + currentTime;// �����Աλ��
							System.out.println(s);
							this.waitEDCasualtyList.remove(this.waitEDCasualtyList.size()-1);
						} else {
							mm++;// ��ʾĿǰED��������waitEDList����mm����Ա��Ҫ�ȴ�
							// break;
						}
					}	
					
				} while (this.waitEDCasualtyList.size() > mm); // ѭ����waitEDCasualtyList�еĶ�����ӵ�EDCasualtyList�У���EDcount�����������

			}
		}
		//ҽԺ��Ա�б��ڰ�����waitEDList֮�����ж��Ƿ����½������Ա������½���Ļ�����Ҫ�ȴ��Ļ����������waitEDList�ȴ���һ��ʱ���ж���ȥ��
		if (this.hospitalCasualtyList != null) {
			if (this.hospitalCasualtyList.size() > 0) {// ��ʾҽԺ����Ա ������Աת������
				int jj = 0;
				do {
					if (ED_Available_Count > 0) {// ����ED�����Խ��ܲ��ˣ�����Ա��ӵ�ED
						EDCasualtyList = getCasualty(
								this.hospitalCasualtyList.get(jj),
								this.EDCasualtyList);
						this.hospitalCasualtyList.get(jj).enterEDTime = currentTime;// ��¼��Ա����ED��ʱ��
						ED_Available_Count--;// ռ��ED��Դ
						// �����Աλ��

						String s = "Hospital " + this.hid + " Casualty "
								+ this.hospitalCasualtyList.get(jj).casualtyID
								+ " Enter the ED at" + currentTime;// �����Աλ��
						System.out.println(s);

						this.hospitalCasualtyList.remove(jj);// ������Ա��HospitalList���Ƴ�

					} else {
						waitEDCasualtyList = getCasualty(
								this.hospitalCasualtyList.get(jj),
								this.waitEDCasualtyList);
//						String s = "Wait: Hospital " + this.hid + " Casualty "
//								+ this.hospitalCasualtyList.get(jj).casualtyID
//								+ " wait ED at" + currentTime;// �����Աλ��
//						System.out.println(s);

						this.hospitalCasualtyList.remove(jj);
					}

				} while (this.hospitalCasualtyList.size() > jj);// ͨ������size�Ĵ�С���е��ڣ�jj=0���䣬��ʾ��ֻҪhospitalCasualtyList������Ա�ͽ���ѭ����ֱ��HospitalList��û����Ա
			}
		}

		// ED���룬��ڣ��������ED�е����в���	
		if (this.EDCasualtyList != null) {
			if (this.EDCasualtyList.size() > 0) {
				int ss = 0;//
				do {
					if (this.EDCasualtyList.get(ss).RPM < 5) {// ������Ա�������أ���Ҫ����ICU
						if (this.ICU_Avaible_Bed_Count > 0) {// ����ICU�п��д�λ��ת����Ա��ICUCasualtyList��
							this.ICUCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.ICUCasualtyList);
							this.EDCasualtyList.get(ss).enterICUTime = currentTime;// ��¼��Ա����ICUʱ��
							this.ICU_Avaible_Bed_Count--;// ռ��ICU��Դ��
							this.ED_Available_Count++;// �ͷ�ED��Դ
							// �����Ա��Ϣ
							String s = "Hospital " + this.hid + " Casualty "
									+ this.EDCasualtyList.get(ss).casualtyID
									+ " Enter the ICU at" + currentTime;// �����Աλ��
							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList�Ƴ���Ա
						} else {// ICUû�п�λ������Աת�Ƶ�waiICUCasualtyList��,���ǲ��ͷ�ED��Դ
							this.waitICUCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.waitICUCasualtyList);
							// �����Ա��Ϣ
//							String s = "Wait: Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " wait the ICU at" + currentTime;// �����Աλ��
//							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList�Ƴ���Ա
						}

					} else if (this.EDCasualtyList.get(ss).RPM < 9) {// ��Ա��5-8֮�䣬����Ҫ����GW
						if (this.GW_Avaible_Bed_Count > 0) {// GW�пմ�λ,����Աת�Ƶ�GWCasualtyList�У�ͬʱ�ͷ�ED��Դ
							this.GWCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.GWCasualtyList);
							this.EDCasualtyList.get(ss).enterGWTime = currentTime;// ��¼��Ա����GWʱ��
							this.GW_Avaible_Bed_Count--;// ռ��GW��Դ
							this.ED_Available_Count++;// �ͷ�ED��Դ

							// �����Աλ����Ϣ
							String s = "Hospital " + this.hid + " Casualty "
									+ this.EDCasualtyList.get(ss).casualtyID
									+ " Enter the GW at" + currentTime;// �����Աλ��
							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList�Ƴ���Ա

						} else {
							this.waitGWCasualtyList = getCasualty(
									this.EDCasualtyList.get(ss),
									this.waitGWCasualtyList);
							// �����Աλ����Ϣ
//							String s = "Wait: Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " wait the GW at" + currentTime;// �����Աλ��
//							System.out.println(s);

							this.EDCasualtyList.remove(ss);// EDCasualtyList�Ƴ���Ա
						}
					} else if (this.EDCasualtyList.get(ss).RPM == 12) {// ��ԱRPM�ﵽ12ʱ�����Ժ
						this.EDCasualtyList.get(ss).stopDeterioration=true;//��Ժ����ֹͣ�����
						this.EDCasualtyList.get(ss).dischargeTime = currentTime;// ��¼��Ա��Ժʱ��
						this.EDCasualtyList.get(ss).overEDTime = currentTime;//��¼����ʱ�� zyh
						this.EDCasualtyList.get(ss).leaveEDTime = currentTime;//��¼ͣ��ʱ�� zyh
						this.EDCasualtyList.get(ss).casualtyPositionFlag = Constants.POSITION_DISCHARGE;// �ı���Աλ��״̬Ϊ
																										// ��Ժ
						String s = "Discharge:Hospital " + this.hid
								+ " Casualty "
								+ this.EDCasualtyList.get(ss).casualtyID
								+ " discharge at" + currentTime;// �����Աλ��
						System.out.println(s);

						this.EDCasualtyList.remove(ss);// ����Ա��ED�б����ͷţ��������ED��Ժ
						this.ED_Available_Count++;// �ͷ�ED��Դ
						// ��¼��Ա��Ժʱ��
					} else {// ��Ա������9-12֮�䣬��ED�н���һ��ʱ������ƾ��ָܻ�
							// ������ֹͣ�����
							// �ж�����ED�е�ͣ��ʱ�䣬�Ƿ�ﵽED��ƽ������ʱ�䣬����ʱ����RPM��ֵΪ12��������Ա��ED��һ��ʱ��󣬳�Ժ��ƽ��ʱ�䰴��15����20minȡ�����
						this.EDCasualtyList.get(ss).stopDeterioration = true;// ֹͣ�����
						double EDtreatTimeDuration = currentTime
								- this.EDCasualtyList.get(ss).enterEDTime;
						Random r = new Random();
						double randomTime = r.nextDouble()*3+5;  //5~8
						if (EDtreatTimeDuration > randomTime) {// ��������Ա��ED�� ÿ
																// 5-8���Ӻ�ת
																// 1�����RPM
							this.EDCasualtyList.get(ss).RPM += 1;
						} else {
							// ������Ա����ED�н��ܴ���
//							String s = "Hospital " + this.hid
//									+ " Casualty "
//									+ this.EDCasualtyList.get(ss).casualtyID
//									+ " treat at ED for" + EDtreatTimeDuration;// �����Աλ��
//							System.out.println(s);
							ss++;
						}
						
						

					}
				} while (this.EDCasualtyList.size() > ss);
			}
		}

		// ICU �ȴ��б�
		if (this.waitICUCasualtyList != null) {
			if (this.waitICUCasualtyList.size() > 0) {
				int xx = 0;
				do {
					if(this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyPositionFlag==Constants.POSITION_DEAD){
						// �����Ա������Ϣ
						String s = "Dead: Hospital " + this.hid
								+ " Casualty "
								+ this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyID
								+ " Dead waiting GW at" + currentTime;// �����Աλ��
						System.out.println(s);
						this.ED_Available_Count++;// �ͷ�ED��Դ
						this.waitICUCasualtyList.remove(this.waitICUCasualtyList.size()-1);
					}else{
						if (this.ICU_Avaible_Bed_Count > 0) {// ��ICU�Ⱥ��б��е���Ա��ת�Ƶ�icu�б���
							this.ICUCasualtyList = getCasualty(
									this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1),
									this.ICUCasualtyList);
							this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).enterICUTime = currentTime;// ��¼��Ա����ICU��ʱ��
							this.ICU_Avaible_Bed_Count--;// ICU��Դռ��
							this.ED_Available_Count++;// �ͷ�ED��Դ
							// �����Ա��Ϣ
							String s = "Hospital " + this.hid + " Casualty "
									+ this.waitICUCasualtyList.get(this.waitICUCasualtyList.size()-1).casualtyID
									+ " enter ICU at" + currentTime;// �����Աλ��
							System.out.println(s);
							//

							this.waitICUCasualtyList.remove(this.waitICUCasualtyList.size()-1);// ���waitICUCasualtyList���еĸ���Ա
						} else {
							xx++;// ��ʾ��ѭ�������� ICU������waitICU�л���xx����Ա
							// break;//֮����Կ�������������ÿһ���ȴ���Ա�ĵȴ�ʱ�䣻

						}
						
						
					}
					
					
				} while (this.waitICUCasualtyList.size() > xx);
			}
		}
		//ICU���,icu�ڲ���ɲ�����
		if (this.ICUCasualtyList != null) {
			if (this.ICUCasualtyList.size() > 0) {
				int rr = 0;
				do {
					if (this.ICUCasualtyList.get(rr).RPM < 5) {
						// ��ʾ��Ա��ICU�н��ܴ��ã���ֹͣ������񻯣�Ȼ������䴦��ʱ�䣬����ƽ������ʱ��󣬸ı���RPM��ʹ����Ա�ת�Ƶ�GW
						this.ICUCasualtyList.get(rr).stopDeterioration = true;// ֹͣ�����
						double ICUtreatTimeDuration = currentTime
								- this.ICUCasualtyList.get(rr).enterICUTime;// ��¼��Ա��ICU�д��õ�ʱ��
						Random r = new Random();
						double randomTime = r.nextDouble()*10+10; // ���Ը��ݲ�ͬҽԺ����趨����ȻĿǰ�Ȳ�������� 10~20
										// �����Բ��ã�����һ�����ʱ��֮�󣬸���ԱRPM+1�ķ�ʽ���ı�������
						if (ICUtreatTimeDuration > randomTime) {
							this.ICUCasualtyList.get(rr).RPM += 1;
						} else {// ��Ա������ICU��������
//							String s = "Hospital " + this.hid + " Casualty "
//									+ this.ICUCasualtyList.get(rr).casualtyID
//									+ " treated at ICU for " + ICUtreatTimeDuration;// �����Աλ��
//							System.out.println(s);
							rr++;
						}
						
					} else {
						// ������Ա���Դ�ICUת��GW��ͨ����
						this.ICUCasualtyList.get(rr).overICUTime= currentTime;// ��¼��ԱICU����ʱ��
						if (this.GW_Avaible_Bed_Count > 0) {// ����GW�д�λ�����Խ���Ա��ICUת��GW
							this.GWCasualtyList = getCasualty(
									this.ICUCasualtyList.get(rr),
									this.GWCasualtyList);
							this.GW_Avaible_Bed_Count--;// ռ��GW��Դ
							this.ICUCasualtyList.get(rr).leaveICUTime = currentTime;// ��¼��Ա�뿪ICUʱ��
							this.ICUCasualtyList.get(rr).enterGWTime = currentTime;// ��¼��Ա����GWʱ��
							this.ICU_Avaible_Bed_Count++;// �ͷ�icu��Դ
							// �����Աλ����Ϣ

							String s = "Hospital " + this.hid + " Casualty "
									+ this.ICUCasualtyList.get(rr).casualtyID
									+ " Enter the GW from ICU at" + currentTime;// �����Աλ��
							System.out.println(s);

							this.ICUCasualtyList.remove(rr);// ������Ա��icuCasualtyList���Ƴ�
						} else {// GW��λ���㣬����Ա�����ڵȴ�GW�б���
							this.waitGWCasualtyList = getCasualty(
									this.ICUCasualtyList.get(rr),
									this.waitGWCasualtyList);

//							String s = "Wait:Hospital " + this.hid
//									+ " Casualty "
//									+ this.ICUCasualtyList.get(rr).casualtyID
//									+ " wait the GW at" + currentTime;// �����Աλ��
//							System.out.println(s);

							this.ICUCasualtyList.remove(rr);
							// this.ICU_Avaible_Bed_Count++;//�ͷ�icu��Դ
						}
					}
				} while (this.ICUCasualtyList.size() > rr);
			}
		}
		// GW���
		if (this.waitGWCasualtyList != null) {
			if (this.waitGWCasualtyList.size() > 0) {
				int ee = 0;
				do {
					if (this.GW_Avaible_Bed_Count > 0) {// �����п��ô�λ����waitGWList�е���Աת�Ƶ�GWlist����
						// ��Ҫ������Աʹ��ICU���Ļ��Ǵ�EDֱ�����ķֿ�����
						if (this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).enterICUTime == 0.0) {// ���waitGWList�У���Ա����ICUʱ��Ϊ0.0����ʾ���Ǵ�EDֱ�ӷ���������
							this.GWCasualtyList = getCasualty(
									this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1),
									this.GWCasualtyList);// ��ת����Ա
							this.GW_Avaible_Bed_Count--;// ռ��GW��Դ
							this.ED_Available_Count++;// �ͷ�ED��Դ

							// �����Աλ����Ϣ
							String s = "Hospital "
									+ this.hid
									+ " Casualty "
									+ this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).casualtyID
									+ " enter the GW at" + currentTime;// �����Աλ��
							System.out.println(s);

							this.waitGWCasualtyList.remove(this.waitGWCasualtyList.size()-1);// ��wait�б����ͷŸ���Ա��
						} else {
							this.GWCasualtyList = getCasualty(
									this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1),
									this.GWCasualtyList);// ��ת����Ա
							this.GW_Avaible_Bed_Count--;// ռ��GW��Դ
							this.ICU_Avaible_Bed_Count++;// �ͷ�ռ��ICU��Դ

							// �����Աλ����Ϣ
							String s = "Hospital "
									+ this.hid
									+ " Casualty "
									+ this.waitGWCasualtyList.get(this.waitGWCasualtyList.size()-1).casualtyID
									+ " enter the GW at" + currentTime;// �����Աλ��
							System.out.println(s);

							this.waitGWCasualtyList.remove(this.waitGWCasualtyList.size()-1);// ��wait�б����ͷŸ���Ա��
						}

					} else {// ����û�д�λ
						ee++;// ��ʾѭ���У�GW��������ee����Ա����waitGWList��
						// break;//����ѭ��
					}

				} while (this.waitGWCasualtyList.size() > ee);
			}

		}
		if (this.GWCasualtyList != null) {
			if (this.GWCasualtyList.size() > 0) {
				int tt = 0;
				do {
					if (this.GWCasualtyList.get(tt).RPM == 12) {
						// ��Ա�������Գ�Ժ
						this.GWCasualtyList.get(tt).dischargeTime = currentTime;// ��¼��Ժʱ��
						this.GWCasualtyList.get(tt).overGWTime = currentTime;// ��¼GW����ʱ��
						this.GWCasualtyList.get(tt).casualtyPositionFlag = Constants.POSITION_DISCHARGE;// �ı���Աλ��״̬
																										// Ϊ��Ժ
						String s = "Discharge:Hospital " + this.hid
								+ " Casualty "
								+ this.GWCasualtyList.get(tt).casualtyID
								+ " discharge at" + currentTime;// �����Աλ��
						System.out.println(s);

						this.GWCasualtyList.remove(tt);// �ͷŸ���Ա
						this.GW_Avaible_Bed_Count++;// �ͷ�GW��Դ
					} else {
						// ��ʾ��Ա��GW���ܴ���
						this.GWCasualtyList.get(tt).stopDeterioration = true;// ֹͣ�����
						double GWTreatTimeDuration = currentTime
								- this.GWCasualtyList.get(tt).enterGWTime;// ��GW�еĴ���ʱ�䣬
						Random r = new Random();
						double randomTime = r.nextDouble()*2+8;// 8~10
						if (GWTreatTimeDuration > randomTime) {
							this.GWCasualtyList.get(tt).RPM += 1;
						} else {// ��Ա
//							String s = "Hospital " + this.hid
//									+ " Casualty "
//									+ this.GWCasualtyList.get(tt).casualtyID
//									+ " treat at GW for" + GWTreatTimeDuration;// �����Աλ��
//							System.out.println(s);
							tt++;
						}
						
					}
				} while (this.GWCasualtyList.size() > tt);
			}
		}

	}

	// ��ȡ��Ա�б���ED��WAITED ����
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
