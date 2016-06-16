package com.nist.ccmserver;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

public class Main{
	
	public TestData data;
	public List<String> usedParams;
	public HashMap pars;
	public String[] infile;
	public String[] values;
	public int[][] test;
	public int[][] hm_colors2;
	public int[] nvals;
	public Boolean[] valSet;
	public int prev_ncols = 0;
	public double[][] bnd;
	public Boolean[] rng;
	public Boolean[] grp;
	public Object[][] group;
	public String[][] map;
	public String[][] SetTest;
	public List<String[][]> aInvalidComb;
    public List<String[][]> aInvalidNotIn;
    public double TotCov2way;      // total coverage for 2-way combinations
    public double TotCov3way;      // total coverage for 3-way combinations
    public double TotCov4way;      // total coverage for 4-way combinations
    public double TotCov5way;      // total coverage for 4-way combinations   
    public double TotCov6way;
    public final int NBINS = 20;
    public int nbnds = 4;
    public Boolean[] boundariesSet; // 1 = all parameters have had boundaries specified
    public static final XYSeriesCollection  chart_data = new XYSeriesCollection();
    public static final XYSeriesCollection bars = new XYSeriesCollection();
    public FileWriter fwRandomFile = null;
    public BufferedWriter bwRandomTests = null;
    public JFreeChart chart;
    public JFreeChart chartcolumn;
    public JFrame frame = new JFrame("CCM");
    public JPanel jPanel = new JPanel();
    public JLabel lblStepChart = new JLabel("");
    public JLabel lblColumnChart = new JLabel("");
    public JLabel lblPointChart = new JLabel("");
    public JPanel pointChartPanel = new JPanel();
    public boolean parallel = false;
    public boolean heatmap = false;
    public boolean stepchart = false;
    public boolean barchart = false;
    public boolean generateMissing = false;
    public boolean appendTests = false;
    public boolean generateRandom = false;
    public int numberOfRandom = 0;
    public String ACTSpath = "";
    public int minCov = 0;
    public String tests_input_file_path = "";
    public String missingCombinationsFilePath = "";
    public static int mode = 1;
    
    public static final int MODE_CLASSIC = 1;
    public static final int MODE_REALTIME = 2;
    
    public static int tway_max = 0;
	
	//can change this or user define it as cmd parameter
	public int nmapMax = 50;
	
	public static void main(String[] args){
		Main m = new Main();
		String constraints_path = "";
		String tway_values[] = new String[5];
		m.data = new TestData();
		/*
		 * Parse the command line arguments
		 */
		int arg_count = 0;
		boolean skip = false;
		for(String s : args){
			
			String param = "";
			if(skip){
				skip = false;
				arg_count++;
				continue;
			}
			String argument = "";
			if(s.charAt(0) == '-' && s.charAt(1) == '-' && s.contains("=")){
				//extended command line name
				argument = s.substring(s.indexOf('=') + 1, s.length());
				param = s.substring(0, s.indexOf("="));
				arg_count++;
			}else if(s.length() == 2 && s.charAt(0) == '-'){
				//shortened command line name
				arg_count++;
				argument = args[arg_count];
				param = s;
				skip = true;
			}else{
				System.out.println("ERROR: Command line arguments not recognized: " + s + "\n\n");
				return;
			}

			switch (param){
			case "--mode":
			case "-M":
				mode = Integer.valueOf(argument);
				break;
			case "--inputfile":
			case "-I":
				m.tests_input_file_path = argument;
				break;
			case "--constraints":
			case "-C":
				constraints_path = argument;
				break;
			case "--param_names":
			case "-P":
				m.data.set_paramNames(Boolean.parseBoolean(argument));
				break;
			case "--ACTSfile":
			case "-A":
				m.ACTSpath = argument;
				m.data.set_acts_file_present(true);
				break;
			case "--parallel":
			case "-p":
				m.parallel = Boolean.parseBoolean(argument);
				break;
			case "--stepchart":
			case "-S":
				m.stepchart = Boolean.parseBoolean(argument);
				break;
			case "--barchart":
			case "-B":
				m.barchart = Boolean.parseBoolean(argument);
				break;
			case "--heatmap":
			case "-H":
				m.heatmap = Boolean.parseBoolean(argument);
				break;
			case "--generate-missing":
			case "-G":
				m.generateMissing = Boolean.parseBoolean(argument);
				break;
			case "--append-tests":
			case "-a":
				m.appendTests = Boolean.parseBoolean(argument);
				break;
			case "--output-file":
			case "-o":
				m.missingCombinationsFilePath = argument;
				break;
			case "--generate-random":
			case "-r":
				m.generateRandom = Boolean.parseBoolean(argument);
				break;
			case "-n":
				m.numberOfRandom = Integer.parseInt(argument);
				break;
			case "--output-random":
			case "-f":
				try {
					m.fwRandomFile = new FileWriter(argument);
					m.bwRandomTests = new BufferedWriter(m.fwRandomFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "--minimum-coverage":
			case "-m":
				m.minCov = Integer.parseInt(argument);
				if(m.minCov <= 0 || m.minCov > 100){
					System.out.println("Can't have a minimum coverage of " + argument);
					m.appendTests = false;
					m.generateMissing = false;
				}
				break;
			case "--tway":
			case "-T":
				String[] vals = argument.split(",");
				tway_values = new String[vals.length];
				for(int i = 0; i < vals.length; i++){
					switch (vals[i]){
					case "2":
						tway_values[i] = "2way";
						if(2 > tway_max)
							tway_max = 2;
						break;
					case "3":
						tway_values[i] = "3way";
						if(3 > tway_max)
							tway_max = 3;
						break;
					case "4":
						tway_values[i] = "4way";
						if(4 > tway_max)
							tway_max = 4;
						break;
					case "5":
						tway_values[i] = "5way";
						if(5 > tway_max)
							tway_max = 5;
						break;
					case "6":
						tway_values[i] = "6way";
						tway_max = 6;
						break;
					default:
						System.out.println("Invalid T-way combination parameter: " + argument);
						return;
					}
				}
				break;
				
			}
		}
		
		/*
		 * End of command line arguments parsing
		 */
		
		if(m.mode == MODE_CLASSIC){
			//Classic mode based off of the GUI version
			m.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			m.lblStepChart.setSize(500,500);
			m.lblColumnChart.setSize(500,500);
			m.lblPointChart.setSize(500,300);
			m.pointChartPanel.add(m.lblPointChart);
			m.frame.add(m.lblStepChart, BorderLayout.WEST);
			m.frame.add(m.lblColumnChart, BorderLayout.EAST);
			m.frame.pack();
			
			//Check and make sure the tests input file is present...
			if(m.tests_input_file_path != ""){
				if (!m.readTestCaseInputFile(m.tests_input_file_path)) {
					return;
				}else{
					//Parameters and values have been auto-detected via the test cases in input file
					
					/*
					 * Now we check the ACTS input file to identify equivalence classes and range values,
					 * as well as identify any possible inputs that weren't specified in auto-detect test cases.
					 * 
					 * Also, check the ACTS file for constraints.
					 */
					
					if(m.data.isActsFilePresent()){
						//check ACTS file
						m.readTestCaseConfigurationFile(m.ACTSpath);
						
						//After processing the ACTS file
						if(constraints_path != "" && !m.data.get_constraints().isEmpty()){
							System.out.println("\nConstraints specified in ACTS file. Using ACTS instead "
									+ "of constraints file.\n");
						}else if(constraints_path != "" && m.data.get_constraints().isEmpty()){
							//Constraints weren't specified in the ACTS file but are specified in constraints file
							m.readConstraintsFile(constraints_path);
							System.out.println("\n\nConstraints: \n\n");
							for(meConstraint c : m.data.get_constraints()){
								System.out.println(c.get_cons());
							}
						}
					}else{
						//No ACTS file present therefore lets check for a constraints file.
						if(constraints_path != ""){
							m.readConstraintsFile(constraints_path);
							
							System.out.println("\n\nConstraints: \n\n");
							for(meConstraint c : m.data.get_constraints()){
								System.out.println(c.get_cons());
							}
							
						}
					}
					
				}
				
			}else{
				if(!m.data.isActsFilePresent()){
					System.out.println("No test case input file nor ACTS file present.\nCan't generate tests "
							+ "without some information on parameters and values.\n");
					return;
				}else{
					//ACTS file is present and the input parameters have been defined.
					if(!m.generateRandom){
						//In classic mode but the user doesn't have a test case file 
						//nor does the user want to generate random tests... something is wrong.
						System.out.println("Incorrect input argumenets specified.");
						return;
					}else{
						//Ok the user wants to generate random tests...
						m.GetRandomTests();
					}
				}
				
			}
			
			//Generate T-way coverage maps
			//The user wants to measure the random tests also...
			for(int i = 0; i < tway_values.length; i++){
				m.Tway(tway_values[i]);
			}
			
			m.frame.pack();
			
		}
		
		
		
		return;
	}

	/*
	 * Zach
	 * 
	 * Reads the test case configuration file. This file should follow the same
	 * format as the ACTS input files specified in the ACTS User manual.
	 * 
	 */
	public boolean readTestCaseConfigurationFile(String path) {
		try{

			List<String> params = new ArrayList<String>();
			List<String> constraints = new ArrayList<String>();
			//List<String[]> values_array = new ArrayList<String[]>();
			List<Integer> types = new ArrayList<Integer>();
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = "";
			Pattern p = Pattern.compile("\\[(.*?)\\]");
			boolean in_constraint_section = false;
			boolean in_param_section = false;
			while((line = br.readLine()) != null){

				line = line.trim();
				Matcher m = p.matcher(line);
				if(m.find()){
					switch(m.group(1)){
					case "Constraint":
						in_constraint_section = true;
						break;
					case "Parameter":
						in_param_section = true;
						break;
					default:
						in_constraint_section = false;
						in_param_section = false;
					}
					continue;
				}

				//Checks to see if the line is a comment
				if(line.startsWith("--"))
					continue;
				
				if(in_constraint_section && !data.isConstraintsFilePresent()){
					//Constraints file isn't present
					//add the line to the constraints...
					constraints.add(line);
					continue;
				}else if(in_param_section && line != ""){
					//do this later...
					
					/*
					 * Need to parse range and boundary values if present.
					 * This section will vary depending on if in CLASSIC Mode 
					 * or REAL TIME Mode.
					 */

					if(mode == MODE_CLASSIC){
						String parameter_name = line.substring(0, line.indexOf("("));
						String value_line = line.substring(line.lastIndexOf(":") + 1, line.length()).trim();
						String[] vals = value_line.split(",");
						
						if (line.contains("*")) {
							// Range value

							//Figure out what index the parameter is
							int parmIndex = 0;
							for(Parameter tp : data.get_parameters()){
								if(parameter_name != tp.getName())
									parmIndex++;
								else
									break;
							}
							//gets all the boundary values...
							String[] boundary_vals = line.substring(line.indexOf("*") + 1, line.length()).trim().replaceAll("\\s", "").split(",");

							
							rng[parmIndex] = true;
							int n = value_line.split(",").length + 1;
							//This just makes sure that at least one value was present in the equivalence class
							if (n < 2) {
								System.out.println("Must have at least 2 values when defining an equivalence class.\n");
								return false;
							}
							Parameter parm = data.get_parameters().get(parmIndex);
							data.get_parameters().remove(parm);
							nbnds = (n - 1 > 0 ? n - 1 : 1);
							nvals[parmIndex] = n;
							if (bnd[parmIndex] == null)
								bnd[parmIndex] = new double[nbnds];
							boundariesSet = new Boolean[nbnds]; // new set of boundaries
																// required since num of
																// values changed
							for (int j = 0; j < nbnds; j++)
								boundariesSet[j] = false;
							
							//Here is where we process the boundary values..
							for (int x = 0; x < boundary_vals.length; x++) {

								try {
									bnd[parmIndex][x] = Double.parseDouble(boundary_vals[x].toString());
									parm.addBound(new java.lang.Double(bnd[parmIndex][x]));
								} catch (Exception ex) {
									System.out.println("Invalid input for boundary value.");
									return false;
								}
								boundariesSet[x] = true; // indicate this bound has been set
								parm.setBoundary(true);
							}
							
							// update in parameters
							parm.setValuesO(parm.getValues());
							parm.removeAllValues();

							for (int b = 0; b <= parm.getBounds().size(); b++) {
								parm.addValue(Integer.toString(b));
							}

							data.get_parameters().add(parmIndex, parm);
							setupFile();


						}else if(line.contains("{")){
							

							//Its a group
							//Figure out what index the parameter is
							int parmIndex = 0;
							for(Parameter tp : data.get_parameters()){
								if(!parameter_name.trim().equals(tp.getName().trim())){
									parmIndex++;
								}
								else{
									grp[parmIndex] = true;
									break;
								}
							}
							//grp[parmIndex] = true;


							/*
							 * get all the groups from the ACTS file line...
							 */
							//
							//Get the information
							List<String> groupDeclarations = new ArrayList<String>();
							String buffer = line.substring(line.indexOf("{"), line.length());
							boolean in_group = false;
							String temp_str = "";
							
							for(char c : buffer.replaceAll("\\s","").trim().toCharArray()){
								if(c == '{'){
									in_group = true;
									continue;
								
								}else if(c == '}'){
									groupDeclarations.add(temp_str);
									temp_str = "";
									in_group = false;
								}else
									temp_str += c;
							}
							
							
							
							//
							//Pattern regexRule = Pattern.compile("[\\{(.*?)\\}]");
							//System.out.println(line);
							//Matcher match = regexRule.matcher(line);
							//List<String> groupDeclarations = new ArrayList<String>();
							//System.out.println("HERE WE GO: ");
							//System.out.println(match.group(1));
					        //while(match.find()) {
					           // groupDeclarations.add(match.group(1));
					       // }

					        List<String[]> all_groups = new ArrayList<String[]>();
					        for(String st : groupDeclarations){
					        	all_groups.add(st.split(","));
					        }

							Parameter tp = data.get_parameters().get(parmIndex);
							nvals[parmIndex] = groupDeclarations.size();
							grp[parmIndex] = true;
							group[parmIndex] = new Object[groupDeclarations.size()];

							tp.setGroup(true);
							tp.setValuesO(tp.getValues());
							tp.removeAllValues();
							for (int index = 0; index < groupDeclarations.size(); index++) {
								group[parmIndex][index] = groupDeclarations.get(index).toString().trim().replaceAll("\\s","");
								tp.addValue(Integer.toString(index));
								tp.addGroup(groupDeclarations.get(index).toString().trim().replaceAll("\\s",""));
							}

							
						
						}else{
							//Looking for values defined in ACTS file that weren't present in the test case file

							for(Parameter par : data.get_parameters()){
								if(par.getName() != parameter_name)
									continue;
								else{
									for(int i = 0; i < vals.length; i++){
										if(!par.getValues().contains(vals[i])){
											//found a new value for this parameter
											par.addValue(vals[i]);
										}
									}
								}
							}
								
						}
						
					}else if(mode == MODE_REALTIME){
						String parameter_name = line.substring(0, line.indexOf("("));
						String type = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
						String value_line = line.substring(line.lastIndexOf(":") + 1, line.length()).trim();
						params.add(parameter_name);
						//values_array.add(value_line.split(","));
						
						switch(type){
						case "enum":
							types.add(1);
							break;
						case "boolean":
							types.add(2);
							break;
						case "int":
							types.add(0);
							break;
						default:
								System.out.println("Incorrect data type : " + type);
								return false;
						}
					}
					
				
				}else
					continue;
			}
			setupFile();
			
			/*
			 * This will all be used later in Real time mode
			 * 
			//Format the parameter names to match the function CreateParameters format
			String tempLine = "";
			for(String str : params){
				tempLine += str;
				tempLine += ",";
			}
			
			data.set_columns(params.size());
			
			
			//This creates all the parameters...

			CreateParameters(params.size(), tempLine.trim().substring(0, tempLine.length() - 1).replaceAll("\\s", ""));
			
			//Time to add the values to the parameters...
			int i = 0;
			for(Parameter tempParam : data.get_parameters()){
				tempParam.setType(types.get(0));
				for(int z = 0; z < values_array.get(i).length; z++){
					//This is where we need to identify and evaluate boundary and group values...
					if(!values_array.get(i)[0].contains("*"))
						tempParam.addValue(values_array.get(i)[z]);
					else{
						//Either a range or group value
						if(values_array.get(i)[0].contains("[")){
							//Group value
							grp[i] = true;
						}else{
							//Range value
							rng[i] = true;
							int n = values_array.get(i).length + 1;
							Parameter parm = data.get_parameters().get(i);
							data.get_parameters().remove(parm);
							int nbnds = (n - 1 > 0 ? n - 1 : 1);
							nvals[i] = n;
							if(n < 2){
								System.out.println("Must have at least 2 values when defining an equivalence class.\n");
								
							}
							
						}
					}
				}
				i++;
			}
			*/
			
			//End of processing
			
			for(String str : constraints)
				AddConstraint(str.trim());
			return true;
		}catch(Exception ex){
			System.out.println(ex.getMessage());
		}
		return false;
	}
	
	
	
	/*
	 * Zach
	 * 
	 * This will read a constraints.txt file if the user provides it.
	 * If both a constraints file and an ACTS file are present, the constraints file
	 * will be used rather than the ACTS file.
	 * 
	 */
	public boolean readConstraintsFile(String path){
		if(data.isActsFilePresent()){
			return false;
		}
		FileInputStream fstream;
		try {
			List<String> constraints = new ArrayList<String>();
			fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while((line = br.readLine()) != null){
				constraints.add(line);
				continue;
			}
			br.close();
			for(String str : constraints)
				AddConstraint(str.trim());
			return true;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

	/*
	 * Zach
	 * 
	 * Reads the test case input file. If the file doesn't exist, one is created
	 * and used for later. 
	 * 
	 * If no ACTS file is present, the program enters auto-detect mode. 
	 * 
	 */
	public boolean readTestCaseInputFile(String path) {

		int lastlen = 0; // used to check that each line of the input file has the same number of columns as the last.

		try {
			
			/*
			 * Read through the file the first time to determine file size and allocate memory.
			 */
			int i = 0;
			int ncols = 0;
			int nrows = 0;
			FileInputStream fstream = new FileInputStream(path);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = "";
			// Read File Line By Line
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				line.trim();
				values = line.split(",");
				int columns = values.length;
				if (data.hasParamNames() && ncols == 0){
					//Essentially auto detection mode for parameter values
					CreateParameters(columns, line);
					ncols = columns;
					continue;
				}else if(!data.hasParamNames() && ncols == 0){
					CreateParameters(columns, "");
					ncols = columns;
					continue;
				}else if(data.hasParamNames() && ncols == 0){
					ncols = data.get_parameters().size();
					continue;
				}
				if (line.contains(",")) {
					values = line.split(",");
					ncols = values.length;
					// Make sure that all rows have the same number of columns.
					if ((i > 0 && ncols != lastlen) || ncols < 2) {
						System.out.println("Invalid input file.\n must have same number of columns all rows");
						br.close();
						return false;
					} else 
						lastlen = ncols;
					i++;
				}

			}

			nrows = i;
			br.close();
			data.set_columns(ncols);
			data.set_rows(nrows);
			values = new String[ncols];
			HashMapParameters();
			infile = new String[data.get_rows()];
			setupParams();
			
		} catch (Exception ex) {
			System.out.println(
					"File cannot be opened for initial test and parameter counting.\nFile may be open in another process."
							+ "\nError: " + ex.getMessage());
			return false;
		}
			/*
			 * Read through the file again to process tests.
			 */
		try{
		    FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = "";
			int i = 0;
			boolean read_params = false;
			while((line = br.readLine()) != null){
				line.trim();
				values = line.split(",");
				int columns = values.length;
				if (data.hasParamNames() && i == 0 && !read_params){
					read_params = true;
					continue;
				}else if (line.contains(",")) {
					values = line.split(",");
					infile[i] = line;
					//if(!data.isActsFilePresent())
					AddValuesToParameters(values);
				}
				i++;
			}
			
			br.close();
			if(setupFile() != 0)
				return false;
		}catch(Exception ex){
			System.out.println("Error: Something went wrong reading the input.csv file.\n" + ex.getMessage());	
		}
		return true;
		
	}

	
	
	/*
	 * Adds a list of values to the parameters.
	 */
	public void AddValuesToParameters(String[] values){
		if (values.length != data.get_parameters().size())
			return;
		Parameter p;
		try {
			for (int x = 0; x < values.length; x++) {
				if (!values[x].trim().equals("")) {
					p = data.get_parameters().get(x);
					if (Tools.isNumeric(values[x])) {
						p.setType(Parameter.PARAM_TYPE_INT);
					} else {
						if (values[x].toString().toUpperCase().equals("TRUE") || values[x].toString().toUpperCase().equals("FALSE")){
							p.setType(Parameter.PARAM_TYPE_BOOL);
						} else {
							p.setType(Parameter.PARAM_TYPE_ENUM);
						}
					}
					p.addValue(values[x]);
				}
			}
		} catch (Exception e) {
			System.out.println("\nError: " + e.getMessage());
		}
	}
	
	
	/*
	 * Create parameter names
	 */
	public void CreateParameters(int number, String parameterNames) { 
		Parameter p;
		String[] names;
		names = parameterNames.split(",");
		for (int i = 0; i < number; i++) {
			// if names are not in file, by default parameters will be named P1, P2, p3...
			if(data.isActsFilePresent()){
				p = new Parameter(names[i]);
			}
			if (!data.hasParamNames() && !data.isActsFilePresent())
				p = new Parameter("P" + (i + 1));
			else
				p = new Parameter(names[i]);
			data.get_parameters().add(p);
		}
	}
	
	
	
	//Add constraint to a list of constraints
	public boolean AddConstraint(String str){

		try {
			String[] x = ConstraintManager.JustParameterValues(str);
			if (Checker(x) && !ConstraintExists(str)) {
				//create parser object
				ConstraintParser cp = new ConstraintParser(str, pars);
				//parse constraint to format choco constraint
				cp.parse();

				// add constraint to list
				data.get_constraints().add(new meConstraint(str, usedParams)); 
				usedParams = null;
				return true;
			} else
				return false;
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return false;
		}
	}
	
	
	
	
	// Key/Value list for parameters, used to solve constraints
	public void HashMapParameters() {
		if(pars == null)
			pars = new HashMap();
		if (pars.size() == 0) {
			for (Parameter p : data.get_parameters()) {
				pars.put(p.getName(), p);
			}
		}
	}
	
	
	
	/*
	 * This will make sure that the constraint is formed with real elements (parameters and its values)
	 */
	protected Boolean Checker(String[] str)
	{
		Boolean r = false;

		HashMap<String, Parameter> lp = new HashMap<String, Parameter>();
		List<String> used_parameters = new ArrayList<String>();

		for (Parameter p : data.get_parameters()) {
			lp.put(p.getName(), p);
		}

		int ant = 0;
		for (int i = 0; i < str.length; i++) {
			if (!str[i].trim().equals("")) {
				//Check if its a variable value
				if (ConstraintManager.isVariableValue(str[i].trim())) { 
					if (i > 0)
						ant = i - 1;
					String key = str[ant];

					Parameter p = lp.get(key.trim());

					if (!ConstraintManager.isValidParameterValue(str[i].trim(), p)) {
						System.out.println("Invalid Value!");
						return false;
					}

				} else {
					//Its a parameter
					if (ConstraintManager.isParameter(str[i].trim(), lp)) {
						if (!used_parameters.contains(str[i].trim()))
							used_parameters.add(str[i].trim());
					} else {
						System.out.println("Invalid Parameter/Value!");
						return false;
					}
				}
			}
		}

		usedParams = used_parameters;
		if (usedParams.size() > 0)
			r = true;
		return r;
	}
	
	//Check to make sure constraints are not repeated
	public Boolean ConstraintExists(String str)
    {
		  for (int c=0; c<data.get_constraints().size();c++){
			 
			  if (data.get_constraints().get(c).get_cons().equalsIgnoreCase(str))
			  {
				  System.out.println("The constriant already exists!!");
				  return true;
			  }
			  
		  }
	
        return false;
    }
	
	
	
	/*
	 * =========================================================================================================
	 * 
	 * Set up arrays for in memory storage of tests
	 * 
	 * =========================================================================================================
	 */
	protected void setupParams() {// creates arrays needed for processing
		try {
			// allocate array for in-memory storage of tests
			test = new int[data.get_rows()][];
			for (int i = 0; i < data.get_rows(); i++) {
				test[i] = new int[data.get_columns()];
			}
			// allocate array for 2-way heat map values
			hm_colors2 = new int[data.get_columns()][];
			for (int i = 0; i < data.get_columns(); i++) {
				hm_colors2[i] = new int[data.get_columns()];
			}

			if (nvals == null || data.get_columns() != prev_ncols) 
				nvals = new int[data.get_columns()];

			if (valSet == null || data.get_columns() != prev_ncols) { 
				valSet = new Boolean[data.get_columns()];
				for (int i = 0; i < data.get_columns(); i++)
					valSet[i] = false;
			} 

			if (bnd == null || data.get_columns() != prev_ncols)
				bnd = new double[data.get_columns()][]; 

			if (rng == null || data.get_columns() != prev_ncols) {
				rng = new Boolean[data.get_columns()]; 
				Arrays.fill(rng, Boolean.FALSE);
			}

			if (group == null || data.get_columns() != prev_ncols)
				group = new Object[data.get_columns()][];

			if (grp == null || data.get_columns() != prev_ncols) {
				grp = new Boolean[data.get_columns()];
				Arrays.fill(grp, Boolean.FALSE);
			}

			if (map == null || data.get_columns() != prev_ncols) { 
				map = new String[data.get_columns()][];
				for (int i = 0; i < data.get_columns(); i++) {
					map[i] = new String[nmapMax];
				}
			}
			prev_ncols = data.get_columns(); 

		} catch (Exception ex) {
			System.out.println("Problem with file or parameter/value specs.\n" + ex.getMessage());
			return;
		}
	}
	
	/*
	 * ======================END OF SETUP PARAMS FUNCTION==================================================
	 */
	
	
	
	
	
	
	
	/*
	
	
	
	/*
	 * Use this function as a means of allocating memory
	 */
	
	 private int setupFile()
     {
		 final int ERR = 5;
         if (infile == null) { System.out.println("Test file must be loaded."); return ERR;}
         if (rng == null)    { System.out.println("Set parameter values."); return ERR; }
        
         
         int i = 0;
         while (i < data.get_rows() ) {
             values = infile[i].split(",");
            
             // locate this value in mapping array; if not found, create and return value
             // determine the index of each input row field and use it in the test array
             // first read in the parameter values for this row as trings      
             
             int j = 0; int m = 0;
             for (m = 0; m < data.get_columns(); m++) {
            	 
                 if (!rng[m] && !grp[m]) {  // discrete values, not range
                	 
						// find value in map array and store its index as value for coverage calculation
                     Boolean fnd = false; int locn = 0;
                     for (j = 0; j < nmapMax && !fnd && map[m][j] != null; j++) {
                         if (map[m][j].equals(values[m])) { fnd = true; locn = j; }
                     }
                     if (data.get_parameters().get(m).getValues().size() > nmapMax) {
                  	   System.out.println("Maximum parameter values exceeded for parameter " + data.get_parameters().get(m).getName() + "=" + data.get_parameters().get(m).getValues().size() + " values." + "\n");
                         
                         return ERR;
                         //nmapMax = parameters[m].getValues().size();
                     } else {// if matching value not in map array, add it and save index
              
                         if (!fnd) { map[m][j] = values[m]; locn = j; }
                         test[i][m] = locn;
                     }
                 } else { 	
              	   
             
              	   
              	   if (rng[m])
              	   {
	                	   // range value, set test[i][m] to input value's interval in range
									// test[i][m] = r  where  boundary[r-1] <= v1 < boundary[r]
	                      double v1=0;
	                       try {	// read in continuous valued variable value
							 v1 = Double.parseDouble(values[m]);
							} catch(Exception ex) { System.out.println("Invalid value for parameter " + m + ".  Value = " + values[m].toString()); return ERR; }
	                       try {
	                       Boolean fnd = false;
	                       
	                       //nbnds = 4;
	                       for (int k = 0; k < nvals[m]-1 && k<nbnds && !fnd; k++) 
								{ if (v1 < bnd[m][k]) { test[i][m] = k; fnd = true; } }
	                       if (!fnd) test[i][m] = nvals[m]-1;    // value > last boundary
	                       } catch(Exception ex) {System.out.println("Problem with file or parameter/value specs in setupfile."); return ERR; }
              	   }
              	   
              	   if (grp[m])
              	   {
              		   //BUG IS HERE.. 
              		   Object v1;
              		   try
              		   {
              			   
              			   v1 = values[m]; 
              		   }
              		   catch(Exception ex){
              			   System.out.println( "Invalid value for parameter " + m + ". Value = " + values[m].toString());
              			   return 2;
              		   }
              		   try
              		   {
              			  Boolean fnd=false;
              			  for(int k=0; k<nvals[m] && !fnd;k++)
              			  {
              				  String[] vals=  group[m][k].toString().split(",");
              				  for (int q=0;q<vals.length;q++)
              				  {

              					  if (vals[q].equals(v1))
              					  {
              						  test[i][m]=k; fnd=true;
              					  }
              				  }
              				  
              			  }
              			   
              			   
              		   }catch(Exception ex)
              		   {
              			   System.out.println("Problem with file or parameter/value specs in setupfile."); return ERR;
              		   }
              		   
              	   }
              	   
                     
                }
            }
             i++;
         }
         
         int j;
         for (i = 0; i < data.get_columns(); i++) { 	// set up number of values for automatically detected parms
             if (!rng[i] && !grp[i]) { 				// count how many value mappings used
                 for (j = 0; j < nmapMax && map[i][j] != null; j++) ;
                 nvals[i] = j;			// j = # of value mappings have been established
             }
         }


         try {
             

             SetTest = new String[data.get_rows()][];

             for (int st = 0; st < data.get_rows(); st++)
             {

                 SetTest[st] = new String[data.get_columns()];

                 for (int v = 0; v < data.get_columns(); v++)
                 {

                     if (!rng[v] && !grp[v])
                     {
                         SetTest[st][v] = map[v][test[st][v]];
                     }
                     else
                     {
                         SetTest[st][v] = Integer.toString(test[st][v]);
                     }
                 }
             }

         return 0;
         } catch(Exception ex) {System.out.println("Problem with file or parameter/value specs."); return ERR; }
         
          
     }

	
	/*
	 * ==============================END OF SETUP FILE FUNCTION================================================
	 */
//}

/*
 * Tway function which handles alot of the heavy lifting...
 * 
 */
		public void Tway(final String t_way) {

			int max = 0;

			switch (t_way) {
			case "2way":
				max = data.get_columns() - 1;
				break;
			case "3way":
				max = data.get_columns() - 2;
				break;
			case "4way":
				max = data.get_columns() - 3;
				break;
			case "5way":
				max = data.get_columns() - 4;
				break;
			case "6way":
				max = data.get_columns() - 5;
				break;

			}

			if (!ValidateTway(t_way))
				return;

			
			final int temp_max = max;
			Thread way = new Thread() {
				@Override
				public void run() {

					Long timeCons1 = System.currentTimeMillis();

					Long timeCons2 = System.currentTimeMillis();

					Long timeway1 = System.currentTimeMillis();

					Tway way = new Tway(t_way, 0,temp_max, test, nvals, data.get_rows(),data.get_columns(),
							data.get_parameters(), data.get_constraints(), map);

					
					//address later with parallel processing
					way.set_Parallel(parallel);
					way.set_bnd(bnd);
					way.set_Rng(rng);
					way.set_group(group);
					way.set_grp(grp);

					/*
					 * For generating missing tests... incorporate later...
					 * 
					 */
					if (generateMissing) {
						way.set_appendTests(appendTests);
						way.set_GenTests(generateMissing);
						way.set_FileNameMissing(missingCombinationsFilePath);
						way.set_appendFile(tests_input_file_path);
						way.set_rptMissingCom(false);
						way.set_minCov(minCov);
						way.set_NGenTests(10000);
						way.set_map(map);
						if(data.hasParamNames())
							way.set_parmName(1);
						else
							way.set_parmName(0);

						//if (rptMissingCom.isSelected())
							//way.set_FileNameReport(fileReport.getPath());

					}
					
					//End of generating missing tests

					ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
					pool.invoke(way);

					Long timeway2 = System.currentTimeMillis();
					Long timewaytotal = timeway2 - timeway1;

					Long timeConsTotal = timeCons2 - timeCons1;

					SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSSSSS");
					df.setTimeZone(TimeZone.getTimeZone("GMT+0"));

					String results = "";

					aInvalidComb = way.get_InvalidComb();
					aInvalidNotIn = way.get_InvalidNotin();
					
					
					/*
					 * Print invalid combinations
					 */
					System.out.println(t_way + " invalid combinations: ");
					for(String[][] str : aInvalidComb){
						for(int i = 0; i < str.length; i++){
							System.out.print(str[i][0] + " = " + str[i][1] + " ; ");
						}
						System.out.print("\n");
					}
					

					//THIS IS THE GRAPHING SECTION WE COULD SAVE UNTIL LATER...
					synchronized(results){
						switch (t_way) {
						case "2way":
							
							hm_colors2 = way.hm_colors2;

							results = graph2way(way._n_tot_tway_cov, way._varvalStatN, way._nComs, way._tot_varvalconfig,
									results, timeConsTotal, timewaytotal, aInvalidComb.size(), aInvalidNotIn.size());

							//FillInvalidDataTable(2);
							break;
						case "3way":
							//hm_colors3 = way.hm_colors3;

							results = graph3way(way._n_tot_tway_cov, way._varvalStatN, way._nComs, way._tot_varvalconfig,
									results, timeConsTotal, timewaytotal, aInvalidComb.size(), aInvalidNotIn.size());
							//FillInvalidDataTable(3);
							break;
						case "4way":

							results = graph4way(way._n_tot_tway_cov, way._varvalStatN, way._nComs, way._tot_varvalconfig,
									results, timeConsTotal, timewaytotal, aInvalidComb.size(), aInvalidNotIn.size());
							//FillInvalidDataTable(4);
							break;
						case "5way":

							results = graph5way(way._n_tot_tway_cov, way._varvalStatN, way._nComs, way._tot_varvalconfig,
									results, timeConsTotal, timewaytotal, aInvalidComb.size(), aInvalidNotIn.size());
							//FillInvalidDataTable(5);
							break;
						case "6way":

							results = graph6way(way._n_tot_tway_cov, way._varvalStatN, way._nComs, way._tot_varvalconfig,
									results, timeConsTotal, timewaytotal, aInvalidComb.size(), aInvalidNotIn.size());
							//FillInvalidDataTable(6);
							break;

						}
					}
					

				}
			};

			way.start();

		}

		private boolean ValidateTway(String tway) {

			switch (tway) {
			case "2way":
				if (data.get_columns() < 2) {
					System.out.println("Cannot compute 2-way coverage for less than 2 parameters");
					return false;
				}
				break;
			case "3way":
				if (data.get_columns() < 3) {
					System.out.println("Cannot compute 3-way coverage for less than 3 parameters");
					return false;
				}
				break;
			case "4way":
				if (data.get_columns() < 4) {
					System.out.println("Cannot compute 4-way coverage for less than 4 parameters");
					return false;
				}
				break;
			case "5way":
				if (data.get_columns() < 5) {
					System.out.println("Cannot compute 5-way coverage for less than 5 parameters");
					return false;
				}
				break;
			case "6way":
				if (data.get_columns() < 6) {
					System.out.println("Cannot compute 6-way coverage for less than 6 parameters");
					return false;
				}
				break;
			}
			
			/*

			if (infile == null) {
				JOptionPane.showMessageDialog(frame, "Load input file first.");
				return false;
			}
			if (setupFile() == ERR)
				return false;
			if (!setupComplete) {
				JOptionPane.showMessageDialog(frame, "Parameter setup did not complete");
				return false;
			}
			if (test == null) {
				JOptionPane.showMessageDialog(frame, "Load input file first.");
				return false;
			}
			if (GenTests.isSelected() && txtMissingFile.getText().trim() == "") {
				JOptionPane.showMessageDialog(frame, "File for missing tests is not specified!");
				return false;
			}
			if (rptMissingCom.isSelected() && txtFileReport.getText().trim() == "") {
				JOptionPane.showMessageDialog(frame, "File for report is not specified!");
				return false;
			}
			if (GenTests.isSelected() && (int) MinCoverPct.getValue() == 0) {
				JOptionPane.showMessageDialog(frame, "For missing combinations is needed specified the coverage!");
				return false;
			}
			*/

			return true;
		}

		
		
		
		/*
		 * ===========================================================================================================
		 * For calculating and graphing the various t-way coverages
		 * ===========================================================================================================
		 * 
		 */
		
		private String graph2way(long n_tot_tway_cov, long[] varvalStats2, long nComs, long tot_varvalconfigs2,
				String results, long tc, long tw, long InvalidIn, long InvalidNot) {
			// ======= display summary statistics ==================

			TotCov2way = ((double) n_tot_tway_cov / (double) tot_varvalconfigs2);

			
			//Add column chart functionality later
			
			if(barchart)
				ColumnChart("2-way", TotCov2way);
			results = "";

			try {
				XYSeries series = new XYSeries("2way", false, true);

				for (int b = NBINS; b >= 0; b--) // drk141007
				{

					double tmpf = (double) (varvalStats2[b]) / (double) (nComs); 

					double tmpfx = (double) b / (double) (NBINS); 

					String tmps = String.format("Cov >= %.2f = %s/%s = %.5f", tmpfx, varvalStats2[b], nComs, tmpf);

					results += tmps;
					series.add(tmpfx, tmpf);

					results += "<br>";
				}

				//if (chart_data.getSeriesIndex(series.getKey()) > -1) {
					//chart_data.removeSeries(chart_data.getSeriesIndex(series.getKey()));
				//}

				chart_data.setIntervalWidth(1.00);

				//XYSeriesCollection s = (XYSeriesCollection) chart_data.clone();

				//chart_data.removeAllSeries();

				chart_data.addSeries(series);
				if(stepchart)
					StepChart(series);
				
				/*
				if (s.getSeriesIndex("6way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("6way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("5way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("5way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("4way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("4way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("3way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("3way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("2way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("2way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				*/

				
				// point chart

				if(!heatmap)
					return results;
				//pointChartPanel.add(panel_5,BorderLayout.NORTH);

				if (data.get_columns() > 100) {
					System.out.println("Heat mat will not shown. Max of 100 parameters for heat map.");
				} else {
					XYSeries red = new XYSeries("red", false, true);
					XYSeries orange = new XYSeries("orange", false, true);
					XYSeries yellow = new XYSeries("yellow", false, true);
					XYSeries green = new XYSeries("green", false, true);
					XYSeries blue = new XYSeries("blue", false, true);

					for (int i = 0; i < data.get_columns() - 1; i++) {
						for (int j = i + 1; j < data.get_columns(); j++) {
							if (hm_colors2[i][j] == 0) {
								red.add(i, j);
							}

							if (hm_colors2[i][j] == 1) {
								orange.add(i, j);
							}
							if (hm_colors2[i][j] == 2)
								yellow.add(i, j);
							if (hm_colors2[i][j] == 3)
								green.add(i, j);
							else
								blue.add(i, j);

						}
					}

					final XYSeriesCollection data2 = new XYSeriesCollection();
					data2.removeAllSeries();

					data2.addSeries(red);
					data2.addSeries(orange);
					data2.addSeries(yellow);
					data2.addSeries(green);
					data2.addSeries(blue);

					final JFreeChart my_chart = ChartFactory.createScatterPlot("", "", "", data2, PlotOrientation.VERTICAL,
							true, false, false);

					XYPlot plot = (XYPlot) my_chart.getPlot();
					LegendItemCollection chartLegend = new LegendItemCollection();
					
					Shape shape = new Rectangle(10,10);
					chartLegend.add(new LegendItem("0-20",null,null,null,shape,Color.RED));
					chartLegend.add(new LegendItem("20-40",null,null,null,shape,Color.ORANGE));
					chartLegend.add(new LegendItem("40-60",null,null,null,shape,Color.YELLOW));
					chartLegend.add(new LegendItem("60-80",null,null,null,shape,Color.GREEN));
					chartLegend.add(new LegendItem("80-100",null,null,null,shape,Color.BLUE));
					plot.setFixedLegendItems(chartLegend);
					plot.getRenderer().setSeriesPaint(data2.indexOf("red"), Color.red);
					plot.getRenderer().setSeriesPaint(data2.indexOf("orange"), Color.orange);
					plot.getRenderer().setSeriesPaint(data2.indexOf("yellow"), Color.yellow);
					plot.getRenderer().setSeriesPaint(data2.indexOf("green"), new Color(34, 177, 76));
					plot.getRenderer().setSeriesPaint(data2.indexOf("blue"), Color.blue);

					Shape cross = ShapeUtilities.createRegularCross(lblPointChart.getWidth() / data.get_columns(),
							lblPointChart.getWidth() / data.get_columns());

					plot.getRenderer().setSeriesShape(data2.indexOf("red"), cross);
					plot.getRenderer().setSeriesShape(data2.indexOf("orange"), cross);
					plot.getRenderer().setSeriesShape(data2.indexOf("yellow"), cross);
					plot.getRenderer().setSeriesShape(data2.indexOf("green"), cross);
					plot.getRenderer().setSeriesShape(data2.indexOf("blue"), cross);

					plot.setBackgroundPaint(new Color(255, 255, 196));

					final NumberAxis domainAxis = new NumberAxis("");

					domainAxis.setRange(0.00, data.get_columns());
					domainAxis.setTickUnit(new NumberTickUnit(data.get_columns() > 10 ? 10 : 1));

					final NumberAxis rangeAxis = new NumberAxis("");
					rangeAxis.setRange(0, data.get_columns());

					rangeAxis.setVisible(false);

					plot.setDomainAxis(0, domainAxis);
					plot.setRangeAxis(0, rangeAxis);

					plot.setDomainCrosshairVisible(true);
					plot.setRangeCrosshairVisible(true);
					
					BufferedImage image = my_chart.createBufferedImage(500,
							400);
					ImageIcon imagen = new ImageIcon(image);

					lblPointChart.setIcon(imagen);
					lblPointChart.repaint();
					pointChartPanel.add(lblPointChart, BorderLayout.SOUTH);

					frame.add(pointChartPanel,BorderLayout.CENTER);
					frame.pack();
					if(!stepchart && !barchart)
						frame.setVisible(true);

				}
				
			} catch (Exception ex) {

				System.out.println(ex.getMessage());

			}

			return results;

		}
		
		/*
		 * ===============================================
		 * END GRAPH 2 - WAY
		 * ===============================================
		 *
		 */
		
		
		/*
		 * ===============================================
		 * GRAPH 3 - WAY
		 * ==============================================
		 * 
		 */
		
		
		private String graph3way(long n_tot_tway_cov, long[] varvalStats3, long nComs, long tot_varvalconfigs3,
				String results, long tc, long tw, long invalidIn, long invalidNot) // drk121109
		{
			// ======= display summary statistics ==================
			TotCov3way = ((double) n_tot_tway_cov / (double) tot_varvalconfigs3);

			// Add row to dtResults

			if(barchart)
				ColumnChart("3-way", TotCov3way);

			results = "";

			try {

				XYSeries series = new XYSeries("3way", false, true);

				// for (int b = 0; b <= NBINS; b++)
				for (int b = NBINS; b >= 0; b--) // drk141007
				{
					double tmpf = (double) varvalStats3[b] / (double) (nComs);
					double tmpfx = (double) b / (double) (NBINS);
					String tmps = String.format("Cov >= %.2f = %s/%s = %.5f", tmpfx, varvalStats3[b], nComs, tmpf);

					results += tmps;

					
					series.add(tmpfx, tmpf);

					results += "<br>";
				}


				//if (chart_data.getSeriesIndex(series.getKey()) > -1) {
					//chart_data.removeSeries(chart_data.getSeriesIndex(series.getKey()));
				//}

				chart_data.setIntervalWidth(1.00);

				//XYSeriesCollection s = (XYSeriesCollection) chart_data.clone();

				//chart_data.removeAllSeries();

				chart_data.addSeries(series);

				
				if(stepchart)
					StepChart(series);
				/*
				if (s.getSeriesIndex("6way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("6way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("5way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("5way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("4way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("4way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("3way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("3way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("2way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("2way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				*/

			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}

		

			return results;

		}
		/*
		 * ==============================================================
		 * END OF GRAPH 3-WAY
		 * ==============================================================
		 */
		
		
		
		/*
		 * 
		 * ===============================================================
		 * GRAPH 4-WAY
		 * ===============================================================
		 */

		private String graph4way(long n_tot_tway_cov, long[] varvalStats4, long nComs, long tot_varvalconfigs4,
				String results, long tc, long tw, long invalidIn, long invalidNot) // drk121109
		{

			// ======= display summary statistics ==================
			TotCov4way = ((double) n_tot_tway_cov / (double) tot_varvalconfigs4);

			// Add row to dtResults
			if(barchart)
				ColumnChart("4-way", TotCov4way);

			results = "";

			try {

				XYSeries series = new XYSeries("4way", false, true);

				for (int b = NBINS; b >= 0; b--) 
				{
					double tmpf = (double) varvalStats4[b] / (double) (nComs);
					double tmpfx = (double) b / (double) (NBINS);
					String tmps = String.format("Cov >= %.2f = %s/%s = %.5f", tmpfx, varvalStats4[b], nComs, tmpf);

					results += tmps;

					series.add(tmpfx, tmpf);

					results += "<br>";
				}


				//if (chart_data.getSeriesIndex(series.getKey()) > -1) {
					//chart_data.removeSeries(chart_data.getSeriesIndex(series.getKey()));
				//}

				chart_data.setIntervalWidth(1.00);

				//XYSeriesCollection s = (XYSeriesCollection) chart_data.clone();

				//chart_data.removeAllSeries();

				chart_data.addSeries(series);

				if(stepchart)
					StepChart(series);
				/*
				if (s.getSeriesIndex("6way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("6way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("5way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("5way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("4way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("4way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("3way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("3way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("2way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("2way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				*/

			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}

			return results;
		}
		
		/*
		 * ========================================================================
		 * END GRAPH 4-WAY
		 * =======================================================================
		 * 
		 */
		
		
		
		/*
		 * ========================================================================
		 * GRAPH 5-WAY
		 * ========================================================================
		 */
		
		private String graph5way(long n_tot_tway_cov, long[] varvalStats5, long nComs, long tot_varvalconfigs5,
				String results, long tc, long tw, long invalidIn, long invalidNot) {

			// ======= display summary statistics ==================
			TotCov5way = ((double) n_tot_tway_cov / (double) tot_varvalconfigs5);
			if(barchart)
				ColumnChart("5-way", TotCov5way);

			results = "";

			try {

				XYSeries series = new XYSeries("5way", false, true);

				for (int b = NBINS; b >= 0; b--) 
				{
					double tmpf = (double) varvalStats5[b] / (double) nComs;
					double tmpfx = (double) b / (double) (NBINS);
					String tmps = String.format("Cov >= %.2f = %s/%s = %.5f", tmpfx, varvalStats5[b], nComs, tmpf);

					results += tmps;

					series.add(tmpfx, tmpf);

					results += "<br>";
				}

				//if (chart_data.getSeriesIndex(series.getKey()) > -1) {

					//chart_data.removeSeries(chart_data.getSeriesIndex(series.getKey()));
				//}

				chart_data.setIntervalWidth(1.00);

				//XYSeriesCollection s = (XYSeriesCollection) chart_data.clone();

				//chart_data.removeAllSeries();

				chart_data.addSeries(series);

				if(stepchart)
					StepChart(series);
				/*
				if (s.getSeriesIndex("6way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("6way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("5way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("5way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("4way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("4way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("3way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("3way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("2way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("2way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				
	*/
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}

			return results;
		}
		
		/*
		 * ================================================================
		 * END OF GRAPH 5-WAY
		 * ================================================================
		 */
		
		
		
		/*
		 * =================================================================
		 * GRAPH 6-WAY
		 * =================================================================
		 */

		private String graph6way(long n_tot_tway_cov, long[] varvalStats6, long nComs, long tot_varvalconfigs6,
				String results, long tc, long tw, long invalidIn, long invalidNot) // drk121109
		{

			// ======= display summary statistics ==================
			TotCov6way = ((double) n_tot_tway_cov / (double) tot_varvalconfigs6);

			if(barchart)
				ColumnChart("6-way", TotCov6way);

			results = "";

			try {

				XYSeries series = new XYSeries("6way", false, true);

				for (int b = NBINS; b >= 0; b--)
				{
					double tmpf = (double) varvalStats6[b] / (double) nComs;
					double tmpfx = (double) b / (double) (NBINS);
					String tmps = String.format("Cov >= %.2f = %s/%s = %.5f", tmpfx, varvalStats6[b], nComs, tmpf);

					results += tmps;

					series.add(tmpfx, tmpf);

					results += "<br>";
				}


				//if (chart_data.getSeriesIndex(series.getKey()) > -1) {
					//chart_data.removeSeries(chart_data.getSeriesIndex(series.getKey()));
				//}

				chart_data.setIntervalWidth(1.00);

				//XYSeriesCollection s = (XYSeriesCollection) chart_data.clone();

				//chart_data.removeAllSeries();

				chart_data.addSeries(series);

				if(stepchart)
					StepChart(series);
				/*
				if (s.getSeriesIndex("6way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("6way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("5way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("5way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("4way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("4way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				if (s.getSeriesIndex("3way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("3way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}

				if (s.getSeriesIndex("2way") > -1) {
					XYSeries serie = s.getSeries(s.getSeriesIndex("2way"));
					chart_data.addSeries(serie);
					StepChart(serie);
				}
				*/
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}

			return results;
		}
		
		/*
		 * ========================================================================
		 * END OF GRAPH 6-WAY
		 * ========================================================================
		 */
		
		
		
		/*
		 * =================================================
		 * PLOTTING THE STEP CHART
		 * =================================================
		 */
		
		
		/*
		 * ========================================================================
		 * END OF GRAPH 6-WAY
		 * ========================================================================
		 */
		
		
		
		/*
		 * =================================================
		 * PLOTTING THE STEP CHART
		 * =================================================
		 */
		
		protected void StepChart(XYSeries serie) {
			
			chart = ChartFactory.createXYStepChart("", "Coverage", "Combinations", chart_data, PlotOrientation.HORIZONTAL, true,
					false, false);
		
			LegendTitle legend = chart.getLegend();
			legend.setPosition(RectangleEdge.RIGHT);
		
			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setDomainAxis(0, new NumberAxis("Combinations")); 
			plot.setRangeAxis(0, new NumberAxis("Coverage")); 
		
			// plot.setRenderer(new XYStepAreaRenderer()); //FILL UNDER THE CURVE
			
			if (chart_data.indexOf("2way") >= 0) {
				plot.getRenderer().setSeriesPaint(chart_data.indexOf("2way"), new Color(237, 28, 36));
				plot.getRenderer().setSeriesStroke(chart_data.indexOf("2way"), new BasicStroke(3.0f));
		
			}
		
			if (chart_data.indexOf("3way") >= 0) {
				plot.getRenderer().setSeriesPaint(chart_data.indexOf("3way"), new Color(63, 72, 204));
				plot.getRenderer().setSeriesStroke(chart_data.indexOf("3way"), new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_MITER, 1.0f, new float[] { 6.0f, 6.0f }, 0.0f));
		
			}
		
			if (chart_data.indexOf("4way") >= 0) {
				plot.getRenderer().setSeriesPaint(chart_data.indexOf("4way"), new Color(34, 177, 76));
				plot.getRenderer().setSeriesStroke(chart_data.indexOf("4way"), new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_ROUND, 1.0f, new float[] { 10.0f, 3.0f, 2.0f, 3.0f }, 0.0f));
			}
			if (chart_data.indexOf("5way") >= 0) {
				plot.getRenderer().setSeriesPaint(chart_data.indexOf("5way"), new Color(210, 105, 30));
				plot.getRenderer().setSeriesStroke(chart_data.indexOf("5way"), new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_ROUND, 1.0f, new float[] { 10.0f, 6.0f, 2.0f, 6.0f, 2.0f, 10.0f }, 0.0f));
			}
			if (chart_data.indexOf("6way") >= 0) {
				plot.getRenderer().setSeriesPaint(chart_data.indexOf("6way"), new Color(139, 0, 139));
				plot.getRenderer().setSeriesStroke(chart_data.indexOf("6way"), new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_ROUND, 1.0f, new float[] { 4.0f, 4.0f, 4.0f, 4.0f, 4.0f }, 2.0f));
			}
		
			plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
			plot.setBackgroundPaint(new Color(255, 255, 196));
			plot.setDomainGridlinesVisible(true);
			plot.setRangeGridlinesVisible(true);
			plot.setDomainGridlinePaint(Color.black);
			plot.setRangeGridlinePaint(Color.black);
		
			NumberAxis axisY = (NumberAxis) plot.getDomainAxis();
		
			axisY.setTickUnit(new NumberTickUnit(0.1d));
		
			axisY.setRange(0D, 1d);
		
			NumberAxis axisX = (NumberAxis) plot.getRangeAxis();
		
			axisX.setRange(0D, 1d);
			axisX.setTickUnit(new NumberTickUnit(0.1d));
		
			BufferedImage image = chart.createBufferedImage(500, 300);
			ImageIcon imagen = new ImageIcon(image);
		
			lblStepChart.setIcon(imagen);
			lblStepChart.repaint();
			
			
			final ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setVisible(true);
			
			frame.pack();
			frame.setVisible(true);
		
		}
		
		/*
		 * =========================================================
		 * END OF PLOTTING STEP CHART
		 * =========================================================
		 */
		
		/*
		 * =========================================================
		 * PLOTTING THE COLUMN CHART
		 * =========================================================
		 */
		
		private void ColumnChart(String tway, double coverage) {

			XYSeries twaySerie = new XYSeries(tway, false, true);

			double x = 0;
			switch (tway) {
			case "2-way":
				x = 1;
				break;
			case "3-way":
				x = 2;
				break;
			case "4-way":
				x = 3;
				break;
			case "5-way":
				x = 4;
				break;
			case "6-way":
				x = 5;
				break;
			}
			twaySerie.add(x, (coverage * 10));

			if (bars.indexOf(tway) >= 0)
				bars.removeSeries(bars.indexOf(tway));

			bars.addSeries(twaySerie);

			chartcolumn = ChartFactory.createXYBarChart("", "t-way", false, "% Coverage", bars, PlotOrientation.VERTICAL,
					true, false, false);

			LegendTitle legend = chartcolumn.getLegend();
			legend.setPosition(RectangleEdge.RIGHT);

			XYPlot plot = (XYPlot) chartcolumn.getPlot();
			plot.setDomainAxis(0, new NumberAxis("t-way"));

			plot.setRangeAxis(0, new NumberAxis("% Coverage"));

			if (bars.indexOf("2-way") >= 0)
				plot.getRenderer().setSeriesPaint(bars.indexOf("2-way"), new Color(237, 28, 36));

			if (bars.indexOf("3-way") >= 0)
				plot.getRenderer().setSeriesPaint(bars.indexOf("3-way"), new Color(63, 72, 204));

			if (bars.indexOf("4-way") >= 0)
				plot.getRenderer().setSeriesPaint(bars.indexOf("4-way"), new Color(34, 177, 76));

			if (bars.indexOf("5-way") >= 0)
				plot.getRenderer().setSeriesPaint(bars.indexOf("5-way"), new Color(210, 105, 30));

			if (bars.indexOf("6-way") >= 0)
				plot.getRenderer().setSeriesPaint(bars.indexOf("6-way"), new Color(139, 0, 139));

			SymbolAxis saX = new SymbolAxis("t-ways", new String[] { "", "2-way", "3-way", "4-way", "5-way", "6-way" });

			SymbolAxis saY = new SymbolAxis("% Coverage",
					new String[] { "0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%" });

			plot.setDomainAxis(saX);
			plot.setRangeAxis(saY);

			plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
			plot.setBackgroundPaint(new Color(255, 255, 196));
			plot.setDomainGridlinesVisible(true);
			plot.setRangeGridlinesVisible(true);
			plot.setDomainGridlinePaint(Color.black);
			plot.setRangeGridlinePaint(Color.black);

			NumberAxis axisY = (NumberAxis) plot.getRangeAxis();

			axisY.setTickUnit(new NumberTickUnit(1d));

			axisY.setRange(0D, 10d);

			NumberAxis axisX = (NumberAxis) plot.getDomainAxis();

			axisX.setRange(0D, 6d);
			axisX.setTickUnit(new NumberTickUnit(1d));

			BufferedImage image = chartcolumn.createBufferedImage(500,
					300);
			ImageIcon imagen = new ImageIcon(image);

			lblColumnChart.setIcon(imagen);
			lblColumnChart.repaint();

			final ChartPanel chartPanel = new ChartPanel(chartcolumn);

			chartPanel.setVisible(true);
			
			frame.pack();
			if(!stepchart)
				frame.setVisible(true);

		}
		
		/*
		 * =========================================================
		 * END OF PLOTTING THE COLUMN CHART
		 * =========================================================
		 */
		
		
		/*
		 * =========================================================
		 * GENERATING RANDOM TESTS
		 * =========================================================
		 */
		
		private void GetRandomTests() {

			try {

				/*
				if (txtInputFileRand.getText().equals("")) {
					JOptionPane.showMessageDialog(frame, "File with parameters isn't loaded!");
					return;
				}
				if (txtOutputFileRand.getText().equals("")) {
					JOptionPane.showMessageDialog(frame, "Output file isn't set!");
					return;
				}
				if ((int) spnNoTestRand.getValue() == 0) {
					JOptionPane.showMessageDialog(frame, "Number of tests must be grather than 0!");
					return;
				}
	*/
				//Thread RandomTests = new Thread() {
					//@Override
					//public void run() {
						CSolver solver = new CSolver();
						
						solver.SetConstraints(data.get_constraints()); // set constraint to
															// solver model
						solver.SetParameter(data.get_parameters()); // set parameters to solver
															// model

						solver.SolverRandomTests((int) numberOfRandom, bwRandomTests);

						//if (chkMeasureCoverage.isSelected()) {
						if(true){
							infile = solver.infile();
							//int ncols = data.get_columns();
							//int nrows = solver.NoRandomTests();
							//txtNumConstraints.setText(Integer.toString(constraints.size()));
							data.set_rows(numberOfRandom);
							setupParams();
							setupFile();
							//tcMain.setEnabledAt(0, true);
							//tcMain.setEnabledAt(1, false);
							//tcMain.setSelectedIndex(0);
							//nrowsBox.setValue(nrows);
							//nColsBox.setValue(ncols);
							//loadComplete = true;
							//FileLoaded(loadComplete);

						}
					//}
				//};

				//RandomTests.start();

			} catch (Exception ex) {
				System.out.println("Error solving constraints");
				return;
			}

		}
		
		/*
		 * =========================================================
		 * END OF GENERATING RANDOM TESTS
		 * =========================================================
		 */
}

