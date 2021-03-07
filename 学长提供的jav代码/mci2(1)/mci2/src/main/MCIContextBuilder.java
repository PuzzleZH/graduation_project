package main;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Random;


import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import agents.Ambulance;
import agents.Casualty;
import agents.Hospital;
import agents.Incident;
import common.Constants;



public class MCIContextBuilder {
	public static Incident inc =new Incident(Constants.MCI_X,Constants.MCI_Y); //�����¹�ʵ��
	public static int currentTime = 0;
	public static List<Hospital> hospitalList=new ArrayList<Hospital>();   //ҽԺ�б�
	public static List<Casualty> casualtyList=new ArrayList<Casualty>();   //��Ա�б�
	public static List<Ambulance> ambulanceList=new ArrayList<Ambulance>();  //���б�
	public static void main(String[] args) {
	//for(int k =1;k<=Constants.REPEAT_TIMES;k++)	{	
		System.out.println("An MCI incident happened at x:"+Constants.MCI_X+" y:"+Constants.MCI_Y);
		 try
	        {  //��xml�ļ��ж�ȡҽԺ��λ����Ϣ
	        	File f=new File("D:\\eclipse-workspace\\mci\\data\\hospital.xml");
	    		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
	    		DocumentBuilder builder=factory.newDocumentBuilder();
	    		Document doc=builder.parse(f);
	    		NodeList nl=doc.getElementsByTagName("hospital");
	    		for(int i=0;i<nl.getLength();++i){
	    			int hx=Integer.parseInt(doc.getElementsByTagName("LOCATION_X").item(i).getFirstChild().getNodeValue().toString());
	    			int hy=Integer.parseInt(doc.getElementsByTagName("LOCATION_Y").item(i).getFirstChild().getNodeValue().toString());
	    			int ii=Integer.parseInt(doc.getElementsByTagName("ID").item(i).getFirstChild().getNodeValue().toString());
	    			Hospital one_hospital=new Hospital(hx,hy,ii);
	    			hospitalList.add(one_hospital);    			
	    			System.out.println("Hospital_"+ii+" at "+"x:"+hx+" y:"+hy);
	    		}
	        }catch(Exception e){
	    		throw new IllegalArgumentException(String.format(e.toString()));	
	    	}
	        
	        //���� �����ڼ��ȳ�����ʼ�ص�̶�
	        
	        try
	        {  //��xml�ļ��ж�ȡҽԺ��λ����Ϣ
	        	File f=new File("D:\\eclipse-workspace\\mci\\data\\AmbulanceBase.xml");
	    		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
	    		DocumentBuilder builder=factory.newDocumentBuilder();
	    		Document doc=builder.parse(f);
	    		NodeList nl=doc.getElementsByTagName("AmbulanceBase");//ȡ�����еļ��ȳ���ʼλ��
	    		int baseNum=0;
	    		int j=0;//���ȳ� ��ʼ���
	    		int ambulances_at_base=0;
	    		do{
	    			ambulances_at_base+=Integer.parseInt(doc.getElementsByTagName("NUM_AMBULANCE").item(baseNum).getFirstChild().getNodeValue().toString());
	    			int hx=Integer.parseInt(doc.getElementsByTagName("LOCATION_X").item(baseNum).getFirstChild().getNodeValue().toString());
	    			int hy=Integer.parseInt(doc.getElementsByTagName("LOCATION_Y").item(baseNum).getFirstChild().getNodeValue().toString());	
	    			do{
	    				Ambulance Ambulance_agent=new Ambulance(hx,hy,j);
	    				ambulanceList.add(Ambulance_agent);    				
	    				System.out.println("Ambulance "+j+" at "+"x:"+hx+" y:"+hy);
	    			
	    				j++;
	    			}while(j<ambulances_at_base);
	    			baseNum++;
	    			
	    		}while(baseNum<nl.getLength());//���ռ��Ȼ�����Ŀ����ѭ��

	    		
	        }catch(Exception e){
	    		throw new IllegalArgumentException(String.format(e.toString()));	
	    	}
	        //������Ա,��ʼ����ԱID������ԱRPM
	        try{
	        	for(int ii=0;ii<Constants.CASUALTY_COUNT;++ii){
	        		Random r = new Random();
	        		int initialRPM = r.nextInt(13);         //��ȫ�����RPM[0,12]
	        		Casualty one_casualty=new Casualty(ii,initialRPM);
	        		casualtyList.add(one_casualty);
	        		System.out.println("Casualty "+ii+" initialRPM is "+one_casualty.RPM+" at Incident");
	        	}
	        	//��ȡ��Ա��RPM�ı仯����¼��Constants.rpmDeterationRecord��
	        	
	        	File rpmcsv=new File("D:\\eclipse-workspace\\mci\\data\\RPM.csv");
	    		//List<String[]> resultList=new ArrayList<String[]>();
	    		@SuppressWarnings("resource")
				BufferedReader br=new BufferedReader(new FileReader(rpmcsv));
	    		String line="";
	    		br.readLine();//��ȡ����
	    		int row=0;//����
	    		while((line=br.readLine())!=null){
	    			String[] elements=line.split(",");
	    			for(int mm=0;mm<elements.length;++mm){
	    				Constants.rpmDeterationRecord[row][mm]=Integer.parseInt(elements[mm].toString());
	    			
	    			}
	    			row++;
	    		}
	    		
	        }catch(Exception e){
	    		throw new IllegalArgumentException(String.format(e.toString()));	
	    	}
	        
	        
	        for(int t=0; t<=Constants.RUN_TIME;++t) {
	        	currentTime = t;
	        	for(int a=0; a<casualtyList.size(); ++a) {
	        		Casualty one = casualtyList.get(a);
	        		
	        		one.step();
	        	}
	        	for(int b=0; b<ambulanceList.size(); ++b) {
	        		Ambulance one = ambulanceList.get(b);
	        		
	        		one.step();
	        	}
	        	
	        	inc.step();
	        	
	        	for(int c=0; c<hospitalList.size(); ++c) {
	        		Hospital one = hospitalList.get(c);
	        		
	        		one.step();
	        	}
	        	
	        }

	}

	//}

}