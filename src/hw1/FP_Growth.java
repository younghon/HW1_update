package hw1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class FP_Growth {
	static double MIN_SUP;
	static int MIN_SUP_COUNT;
	static double MIN_CONF;
	static String finPath;
	static BufferedWriter bw;
	static Hashtable<String, Integer>  L1;
	static Hashtable<String, Node> headerTable;
	static HashMap<String,Integer> fPatterns;
	static double MaxMemory;
	static int frequentpattern = 0;
	static int rules = 0;
	/**
	 * @param args 
	 */
	public static void main(String[] args) throws IOException {
		//請使用者輸入參數input file/min_sup/min_conf
		Scanner input = new Scanner(System.in);
		//System.out.print("請輸入input file(D1kT10N500.txt/D10kT10N1k.txt/D100kT10N1k.txt/Mushroom.txt):");
		finPath = args[0];
		//System.out.print("請輸入Min_Sup:");
		MIN_SUP = Double.parseDouble(args[1]);
		//System.out.print("請輸入Min_Conf:");
		MIN_CONF = Double.parseDouble(args[2]);
		
		
		MaxMemory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		int transaction_count = 0;
		long startTime = System.currentTimeMillis();
		Node root = new Node();
		L1 = new Hashtable<String, Integer>();
		headerTable = new Hashtable<String, Node>();
		fPatterns = new HashMap<String,Integer>();
		
		//讀檔 建立L1
		try {
			Scanner inputScanner = new Scanner(new File(finPath));
			while(inputScanner.hasNextLine()){
					transaction_count++;
					String line = inputScanner.nextLine();
					String[] tokens = line.split(", ");
					for(int i=0;i<tokens.length;i++){
						if(!L1.containsKey(tokens[i])){
							L1.put(tokens[i], 1);
						}else{
							L1.put(tokens[i], L1.get(tokens[i])+1);
						}
					}
			}
			inputScanner.close();
		} catch(IOException e) {
				e.printStackTrace();
		}
		
		//印出資料總筆數 與 Min Support count
		MIN_SUP_COUNT = (int) Math.ceil(transaction_count*MIN_SUP);
		System.out.println("Transaction筆數: "+transaction_count);
		System.out.println("Min Support count: "+ MIN_SUP_COUNT);
		
		//刪除infrequent item
		Enumeration<String> enumeration = L1.keys();
		while(enumeration.hasMoreElements()){
			String key = enumeration.nextElement();
			if(L1.get(key) < MIN_SUP_COUNT){
				//System.out.println(key+": "+L1.get(key)+"  小於min support count...");
				L1.remove(key);					
			}
		}
		
		double current_memory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		if(current_memory>MaxMemory)
			MaxMemory = current_memory;
		
		/*依照count大小印出transaction
		List<Map.Entry<String, Integer>> list_Data = new ArrayList<Map.Entry<String, Integer>>(L1.entrySet());
		Collections.sort(list_Data, new Comparator<Map.Entry<String, Integer>>() {    
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {    
                return (o2.getValue() - o1.getValue());
            }    
        });  
		for (Map.Entry<String, Integer> entry:list_Data) {
            System.out.println(entry.getKey()+": "+entry.getValue());
        }
        System.out.println();*/
                
        
        //建FP_Tree與headtable
    	try {
			Scanner inputScanner = new Scanner(new File(finPath));
			while(inputScanner.hasNextLine()){
					String line = inputScanner.nextLine();
					String s = sort_by_L1(L1,line);
					String[] tmp = s.split(", ");       	       	
		        	Node p = root;
		        	
		        	for(int k=0;k<tmp.length;k++){
    		        		Node newNode;
    						if(p.child.containsKey(tmp[k])){
    							newNode = p.child.get(tmp[k]);
    							newNode.count++;
    						}else{
    							newNode = new Node();
    							newNode.item = tmp[k];
    							newNode.count = 1;
    							newNode.parent = p;
    							p.child.put(tmp[k], newNode);
    							
    							if(headerTable.get(tmp[k]) != null){
    								newNode.header = headerTable.get(tmp[k]);
    							}
    							headerTable.put(tmp[k], newNode);	
    						}
    						p = newNode;
		        	}
			}
			inputScanner.close();
			
			current_memory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
			if(current_memory>MaxMemory)
				MaxMemory = current_memory;
			
			//call funtion並將所有frequent Itemset結果寫入檔案
			bw = new BufferedWriter(new FileWriter(finPath+"_sup"+MIN_SUP+"_frqItemset.txt"));
			retrieveFrqItemset(root, headerTable);
			bw.close();
		} catch(IOException e) {
				e.printStackTrace();
		}
       

		bw = new BufferedWriter(new FileWriter(finPath+"_sup"+MIN_SUP+"_conf"+MIN_CONF+"_rule.txt"));
    	Set<String> keySet = fPatterns.keySet();	
    	try{
			for(String key : keySet){
				if (!key.contains(",")) {    
					continue;
				}
				double oSup = (double) fPatterns.get(key);
				String[] oItemsets = key.split(", ");  
				
				findRule("", 0, oSup, MIN_CONF, oItemsets, key, bw);
			}
			bw.close();
    	}catch(IOException e) {
			e.printStackTrace();
    	}
    	
    	current_memory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		if(current_memory>MaxMemory)
			MaxMemory = current_memory;
    	
		//寫入.csv檔
		bw = new BufferedWriter(new FileWriter("exp/"+args[0]+"_result.csv",true));
		bw.write("Minimum Support/Confidence,# of Frequent Patterns,# of Rules,Total Time,Maximum Memory");
		bw.newLine();
		
		bw.write((MIN_SUP/MIN_CONF)+","+frequentpattern+","+rules+","+((System.currentTimeMillis() - startTime)/1000.0)+","+MaxMemory);
		bw.newLine();
		bw.close();
		
		//在screen上顯示出資訊
    	/*
		System.out.println("# of frequent pattern: "+frequentpattern);
    	System.out.println("# of rule: "+rules);
    	System.out.println("執行時間: "+(System.currentTimeMillis() - startTime)/1000.0);
    	System.out.println("記憶體: "+ MaxMemory);
        */
	}
	
	public static String sort_by_L1(Hashtable<String,Integer> hashtable,String s){
		HashMap <String,Integer> tmp = new HashMap <String, Integer>();
		
		String[] tokens = s.split(", ");
		for(int i=0;i<tokens.length;i++){
			if(hashtable.containsKey(tokens[i]))
				tmp.put(tokens[i], hashtable.get(tokens[i]));
		}
		
		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(tmp.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {    
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {    
                return (o2.getValue() - o1.getValue());  
            }    
        }); 
		
		String Sorted="";
		for (Map.Entry<String, Integer> entry:list) {
            //System.out.println(entry.getKey()+", ");
			Sorted += entry.getKey()+", ";
		}
		return Sorted;
	}
	
	
	public static void retrieveFrqItemset(Node treeRoot, Hashtable<String, Node> headerTable){
		ArrayList<String> sArrayList = new ArrayList<String>(headerTable.keySet());
		for (int i = 0 ; i < sArrayList.size() ; i++) {
			Node node = headerTable.get(sArrayList.get(i));
			
			int support = 0;
			while(node != null) {
				support += node.count;
				node = node.header;				
			}
			if (support >= MIN_SUP_COUNT) {
				String frqItemset = treeRoot.item + ", " + sArrayList.get(i);				
				String[] tmp = frqItemset.substring(2).split(", ");
				int[] tmpToInt = new int[tmp.length];
				for(int k = 0 ; k < tmp.length ; k++){
					tmpToInt[k] = Integer.parseInt(tmp[k]);
				}
				Arrays.sort(tmpToInt);
				frqItemset = "";
				for (int j = 0; j < tmp.length; j++)
					frqItemset = frqItemset + ", " + tmpToInt[j];
				
				//System.out.println(frqItemset.substring(2) + " : " + support); //
				try{		
					bw.write(frqItemset.substring(2) + " : " + support);
					bw.newLine();
				}catch(IOException e){
				}
				frequentpattern++;
				fPatterns.put(frqItemset.substring(2), support);

				
				// build conditional tree and header table //
				Node condtreeRoot = new Node();
				Hashtable<String, Node> condHeaderTable = new Hashtable<String, Node>();
				buildCondtree(condtreeRoot, treeRoot, condHeaderTable, headerTable, sArrayList.get(i));
				
				if(!condtreeRoot.child.isEmpty()){
					retrieveFrqItemset(condtreeRoot, condHeaderTable);
				}else{
				    // eliminate conditional tree and header table //
				    condtreeRoot = null;
				    condHeaderTable.clear();
				}
			}
		}
		// eliminate tree and header table //
		treeRoot = null;
		headerTable.clear();
	}

	public static void buildCondtree(Node condtreeRoot, Node treeRoot, Hashtable<String, Node> condHeaderTable, Hashtable<String, Node> headerTable, String item){
		condtreeRoot.item = treeRoot.item + ", " + item;
		Node node = headerTable.get(item);
		while(node != null) {
			Node upwardnode = node.parent;
			ArrayList<String> items = new ArrayList<String>();
			while(upwardnode.parent != null){
				//line = line + ", " + upwardnode.item;
				items.add(0, upwardnode.item);
				upwardnode = upwardnode.parent;
			}
            Integer support = node.count;
			// conditional transaction support count //
			Node downwardnode = condtreeRoot;
			for (int i = 0; i < items.size(); i++) {
				Node newnode = getChild(downwardnode.child, items.get(i));
				if(newnode == null){
					newnode = new Node();
					newnode.item = items.get(i);
					newnode.count = support;
					newnode.parent = downwardnode;
					downwardnode.child.put(items.get(i), newnode);
			
					if(condHeaderTable.containsKey(items.get(i))){
						newnode.header = condHeaderTable.get(items.get(i));
					}
					condHeaderTable.put(items.get(i), newnode);
				}else{
					newnode.count = newnode.count + support;
				}
				downwardnode = newnode;
			}			
			node = node.header;
		}
    }
	
	public static Node getChild(HashMap<String, Node> child, String str){
		if (child.containsKey(str)){
			return child.get(str);
		}else return null;    	
    }
	
	public static void findRule(String prefix, int sPosition, double oSup, double min_conf, String[] oItemsets, String okey, BufferedWriter bfWriter) throws IOException{
		for (int i = sPosition; i < oItemsets.length; i++) {
			String nextPrefix = (prefix == "" ? oItemsets[i]: prefix + ", " + oItemsets[i]);
			if (nextPrefix.equals(okey)){  //String掀誕岆瘁眈脹猁蚚equals
				continue;
			}
			double conf = oSup/(fPatterns.get(nextPrefix));
			if (conf >= min_conf) {
				//怀堤涴沭rule
				rules++;
				bfWriter.write("["+ nextPrefix + "] -> [" + okey + "]: confidence = " + Math.round(conf*1000)/1000.0); 
				bfWriter.newLine();
			}
			int nextSPosition = i + 1;
			if (nextSPosition < oItemsets.length) {
				findRule(nextPrefix, nextSPosition, oSup, min_conf, oItemsets, okey, bfWriter);
			}
			
		}
	}
	
	public static class Node{
		String item;
		int count;
		Node header = null;
		Node parent = null;
		HashMap <String, Node> child = new HashMap<String, Node>();
		Node(){
			item = "";
		}
	}
}
