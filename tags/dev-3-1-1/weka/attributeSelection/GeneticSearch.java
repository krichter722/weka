/*
 *    GeneticSearch.java
 *    Copyright (C) 1999 Mark Hall
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;

/** 
 * Class for performing a genetic based search. <p>
 *
 * For more information see: <p>
 * David E. Goldberg (1989). Genetic algorithms in search, optimization and
 * machine learning. Addison-Wesley. <p>
 *
 * Valid options are: <p>
 *
 * -P <size of the population> <br>
 * Sets the size of the population. (default = 20). <p>
 *
 * -G <number of generations> <br>
 * Sets the number of generations to perform.
 * (default = 5). <p>
 *
 * -C <probability of crossover> <br>
 * Sets the probability that crossover will occur.
 * (default = .6). <p>
 *
 * -M <probability of mutation> <br>
 * Sets the probability that a feature will be toggled on/off. <p>
 *
 * -R <report frequency> <br>
 * Sets how frequently reports will be generated. Eg, setting the value
 * to 5 will generate a report every 5th generation. <p>
 * (default = number of generations). <p>
 *
 * -S <seed> <br>
 * Sets the seed for random number generation. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class GeneticSearch extends ASSearch implements OptionHandler {

  /** 
   * holds a starting set (if one is supplied). Becomes one member of the
   * initial random population
   */
  private int[] m_starting;
  
 /** does the data have a class */
  private boolean m_hasClass;
 
  /** holds the class index */
  private int m_classIndex;
 
  /** number of attributes in the data */
  private int m_numAttribs;

  /** the current population */
  private GABitSet [] m_population;

  /** the number of individual solutions */
  private int m_popSize;

  /** the best population member found during the search */
  private GABitSet m_best;

  /** the number of features in the best population member */
  private int m_bestFeatureCount;

  /** the number of entries to cache for lookup */
  private int m_lookupTableSize;

  /** the lookup table */
  private Hashtable m_lookupTable;

  /** random number generation */
  private Random m_random;

  /** seed for random number generation */
  private int m_seed;

  /** the probability of crossover occuring */
  private double m_pCrossover;

  /** the probability of mutation occuring */
  private double m_pMutation;

  /** sum of the current population fitness */
  private double m_sumFitness;

  private double m_maxFitness;
  private double m_minFitness;
  private double m_avgFitness;

  /** the maximum number of generations to evaluate */
  private int m_maxGenerations;

  /** how often reports are generated */
  private int m_reportFrequency;

  /** holds the generation reports */
  private StringBuffer m_generationReports;

  // Inner class
  protected class GABitSet implements Cloneable {
    
    private BitSet m_chromosome;

    /** holds raw merit */
    private double m_objective = -Double.MAX_VALUE;
    private double m_fitness;

    /**
     * Constructor
     */
    public GABitSet () {
      m_chromosome = new BitSet();
    }

    /**
     * makes a copy of this GABitSet
     * @return a copy of the object
     * @exception if something goes wrong
     */
    public Object clone() throws CloneNotSupportedException {
      GABitSet temp = new GABitSet();
      
      temp.setObjective(this.getObjective());
      temp.setFitness(this.getFitness());
      temp.setChromosome((BitSet)(this.m_chromosome.clone()));
      return temp;
      //return super.clone();
    }

    /**
     * sets the objective merit value
     * @param objective the objective value of this population member
     */
    public void setObjective(double objective) {
      m_objective = objective;
    }
      
    /**
     * gets the objective merit
     * @return the objective merit of this population member
     */
    public double getObjective() {
      return m_objective;
    }

    /**
     * sets the scaled fitness
     * @param the scaled fitness of this population member
     */
    public void setFitness(double fitness) {
      m_fitness = fitness;
    }

    /**
     * gets the scaled fitness
     * @return the scaled fitness of this population member
     */
    public double getFitness() {
      return m_fitness;
    }

    /**
     * get the chromosome
     * @returns the chromosome of this population member
     */
    public BitSet getChromosome() {
      return m_chromosome;
    }

    /**
     * set the chromosome
     * @param the chromosome to be set for this population member
     */
    public void setChromosome(BitSet c) {
      m_chromosome = c;
    }

    /**
     * unset a bit in the chromosome
     * @param bit the bit to be cleared
     */
    public void clear(int bit) {
      m_chromosome.clear(bit);
    }

    /**
     * set a bit in the chromosome
     * @param bit the bit to be set
     */
    public void set(int bit) {
      m_chromosome.set(bit);
    }

    /**
     * get the value of a bit in the chromosome
     * @param bit the bit to query
     * @return the value of the bit
     */
    public boolean get(int bit) {
      return m_chromosome.get(bit);
    }
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(6);
    newVector.addElement(new Option("\tSet the size of the population."
				    +"\n\t(default = 10)."
				    , "P", 1
				    , "-P <population size>"));
    newVector.addElement(new Option("\tSet the number of generations."
				    +"\n\t(default = 20)" 
				    , "G", 1, "-G <number of generations>"));
    newVector.addElement(new Option("\tSet the probability of crossover."
				    +"\n\t(default = 0.6)" 
				    , "C", 1, "-C <probability of"
				    +" crossover>"));    
    newVector.addElement(new Option("\tSet the probability of mutation."
				    +"\n\t(default = 0.033)" 
				    , "M", 1, "-M <probability of mutation>"));

    newVector.addElement(new Option("\tSet frequency of generation reports."
				    +"\n\te.g, setting the value to 5 will "
				    +"\n\t report every 5th generation"
				    +"\n\t(default = number of generations)" 
				    , "R", 1, "-R <report frequency>"));
    newVector.addElement(new Option("\tSet the random number seed."
				    +"\n\t(default = 1)" 
				    , "S", 1, "-S <seed>"));
    return  newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -P <size of the population> <br>
   * Sets the size of the population. (default = 20). <p>
   *
   * -G <number of generations> <br>
   * Sets the number of generations to perform.
   * (default = 5). <p>
   *
   * -C <probability of crossover> <br>
   * Sets the probability that crossover will occur.
   * (default = .6). <p>
   *
   * -M <probability of mutation> <br>
   * Sets the probability that a feature will be toggled on/off. <p>
   *
   * -R <report frequency> <br>
   * Sets how frequently reports will be generated. Eg, setting the value
   * to 5 will generate a report every 5th generation. <p>
   * (default = number of generations). <p>
   *
   * -S <seed> <br>
   * Sets the seed for random number generation. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception
  {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setPopulationSize(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('G', options);
    if (optionString.length() != 0) {
      setMaxGenerations(Integer.parseInt(optionString));
      setReportFrequency(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('C', options);
    if (optionString.length() != 0) {
      setCrossoverProb((new Double(optionString)).doubleValue());
    }

    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMutationProb((new Double(optionString)).doubleValue());
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setReportFrequency(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[12];
    int current = 0;

    options[current++] = "-P";
    options[current++] = "" + getPopulationSize();
    options[current++] = "-G";
    options[current++] = "" + getMaxGenerations();
    options[current++] = "-C";
    options[current++] = "" + getCrossoverProb();
    options[current++] = "-M";
    options[current++] = "" + getMutationProb();
    options[current++] = "-R";
    options[current++] = "" + getReportFrequency();
    options[current++] = "-S";
    options[current++] = "" + getSeed();

    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
  }
  
  /**
   * set the seed for random number generation
   * @param s seed value
   */
  public void setSeed(int s) {
    m_seed = s;
  }

  /**
   * get the value of the random number generator's seed
   * @return the seed for random number generation
   */
  public int getSeed() {
    return m_seed;
  }

  /**
   * set how often reports are generated
   * @param f generate reports every f generations
   */
  public void setReportFrequency(int f) {
    m_reportFrequency = f;
  }

  /**
   * get how often repports are generated
   * @return how often reports are generated
   */
  public int getReportFrequency() {
    return m_reportFrequency;
  }

  /**
   * set the probability of mutation
   * @param m the probability for mutation occuring
   */
  public void setMutationProb(double m) {
    m_pMutation = m;
  }

  /**
   * get the probability of mutation
   * @return the probability of mutation occuring
   */
  public double getMutationProb() {
    return m_pMutation;
  }

  /**
   * set the probability of crossover
   * @param c the probability that two population members will exchange
   * genetic material
   */
  public void setCrossoverProb(double c) {
    m_pCrossover = c;
  }

  /**
   * get the probability of crossover
   * @return the probability of crossover
   */
  public double getCrossoverProb() {
    return m_pCrossover;
  }

  /**
   * set the number of generations to evaluate
   * @param m the number of generations
   */
  public void setMaxGenerations(int m) {
    m_maxGenerations = m;
  }

  /**
   * get the number of generations
   * @return the maximum number of generations
   */
  public int getMaxGenerations() {
    return m_maxGenerations;
  }
  
  /**
   * set the population size
   * @param p the size of the population
   */
  public void setPopulationSize(int p) {
    m_popSize = p;
  }

  /**
   * get the size of the population
   * @return the population size
   */
  public int getPopulationSize() {
    return m_popSize;
  }

  /**
   * Constructor. Make a new GeneticSearch object
   */
  public GeneticSearch() {
    resetOptions();
  }

  /**
   * returns a description of the search
   * @return a description of the search as a String
   */
  public String toString() {
    StringBuffer GAString = new StringBuffer();
    GAString.append("\tGenetic search.\n\tStart set: ");

    if (m_starting == null) {
      GAString.append("no attributes\n");
    }
    else {
      boolean didPrint;

      for (int i = 0; i < m_starting.length; i++) {
	didPrint = false;

	if ((m_hasClass == false) || 
	    (m_hasClass == true && i != m_classIndex)) {
	  GAString.append((m_starting[i] + 1));
	  didPrint = true;
	}

	if (i == (m_starting.length - 1)) {
	  GAString.append("\n");
	}
	else {
	  if (didPrint) {
	    GAString.append(",");
	  }
	}
      }
    }
    GAString.append("\tPopulation size: "+m_popSize);
    GAString.append("\n\tNumber of generations: "+m_maxGenerations);
    GAString.append("\n\tProbability of crossover: "
		+Utils.doubleToString(m_pCrossover,6,3));
    GAString.append("\n\tProbability of mutation: "
		+Utils.doubleToString(m_pMutation,6,3));
    GAString.append("\n\tReport frequency: "+m_reportFrequency);
    GAString.append("\n\tRandom number seed: "+m_seed+"\n");
    GAString.append(m_generationReports.toString());
    return GAString.toString();
  }

  /**
   * Searches the attribute subset space using a genetic algorithm.
   *
   * @param startSet a (possibly) ordered array of attribute indexes from
   * which to start the search from. Set to null if no explicit start
   * point.
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
   public int[] search (int[] startSet, ASEvaluation ASEval, Instances data)
    throws Exception {

     m_best = null;
     m_generationReports = new StringBuffer();

     if (!(ASEval instanceof SubsetEvaluator)) {
       throw  new Exception(ASEval.getClass().getName() 
			    + " is not a " 
			    + "Subset evaluator!");
     }
     
    if (startSet != null) {
      m_starting = startSet;
    }

    if (ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
    }
    else {
      m_hasClass = true;
      m_classIndex = data.classIndex();
    }

    SubsetEvaluator ASEvaluator = (SubsetEvaluator)ASEval;
    m_numAttribs = data.numAttributes();

    // initial random population
    m_lookupTable = new Hashtable(m_lookupTableSize);
    m_random = new Random(m_seed);
    m_population = new GABitSet [m_popSize];

    // set up random initial population
    initPopulation();
    evaluatePopulation(ASEvaluator);
    populationStatistics();
    scalePopulation();
    checkBest();
    m_generationReports.append(populationReport(0));

    boolean converged;
    for (int i=1;i<=m_maxGenerations;i++) {
      generation();
      evaluatePopulation(ASEvaluator);
      populationStatistics();
      scalePopulation();
      // find the best pop member and check for convergence
      converged = checkBest();

      if ((i == m_maxGenerations) || 
	  ((i % m_reportFrequency) == 0) ||
	  (converged == true)) {
	m_generationReports.append(populationReport(i));
	break;
      }
    }
    return attributeList(m_best.getChromosome());
   }

  /**
   * converts a BitSet into a list of attribute indexes 
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  private int[] attributeList (BitSet group) {
    int count = 0;

    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	list[count++] = i;
      }
    }

    return  list;
  }

  /**
   * checks to see if any population members in the current
   * population are better than the best found so far. Also checks
   * to see if the search has converged---that is there is no difference
   * in fitness between the best and worse population member
   * @return true is the search has converged
   * @exception if something goes wrong
   */
  private boolean checkBest() throws Exception {
    int i,j,count,lowestCount = m_numAttribs;
    double b = -Double.MAX_VALUE;
    GABitSet localbest = null;
    BitSet temp;
    boolean converged = false;

    if (m_maxFitness - m_minFitness > 0) {
      // find the best in this population
      for (i=0;i<m_popSize;i++) {
	if (m_population[i].getObjective() > b) {
	  b = m_population[i].getObjective();
	  localbest = m_population[i];
	} 
      }
    } else {
      // look for the smallest subset
      for (i=0;i<m_popSize;i++) {
	temp = m_population[i].getChromosome();
	count = 0;
	for (j=0;j<m_numAttribs;j++) {
	  if (temp.get(j)) {
	    count++;
	  }
	}
	if (count < lowestCount) {
	  lowestCount = count;
	  localbest = m_population[i];
	  b = localbest.getObjective();
	}
      }
      converged = true;
    }

    // count the number of features in localbest
    count = 0;
    temp = localbest.getChromosome();
    for (j=0;j<m_numAttribs;j++) {
      if (temp.get(j)) {
	count++;
      }
    }

    // compare to the best found so far
    if (m_best == null) {
      m_best = (GABitSet)localbest.clone();
      m_bestFeatureCount = count;
    } else if (b > m_best.getObjective()) {
      m_best = (GABitSet)localbest.clone();
      m_bestFeatureCount = count;
    } else if (Utils.eq(m_best.getObjective(), b)) {
      // see if the localbest has fewer features than the best so far
      if (count < m_bestFeatureCount) {
	m_best = (GABitSet)localbest.clone();
	m_bestFeatureCount = count;
      }
    }
    return converged;
  }

  /**
   * performs a single generation---selection, crossover, and mutation
   * @exception if an error occurs
   */
  private void generation () throws Exception {
    int i,j=0;
    double best_fit = 0.0;
    GABitSet [] newPop = new GABitSet [m_popSize];
    int parent1,parent2;

    /** first ensure that the population best is propogated into the new
	generation */
    for (i=0;i<m_popSize;i++) {
      if (m_population[i].getFitness() > best_fit) {
	j = i;
	best_fit = m_population[i].getFitness();
      }
    }
    newPop[0] = (GABitSet)(m_population[j].clone());
    newPop[1] = newPop[0];

    for (j=2;j<m_popSize;j+=2) {
      parent1 = select();
      parent2 = select();

      newPop[j] = (GABitSet)(m_population[parent1].clone());
      newPop[j+1] = (GABitSet)(m_population[parent2].clone());
      // if parents are equal mutate one bit
      if (parent1 == parent2) {
	int r;
	if (m_hasClass) {
	  while ((r = (Math.abs(m_random.nextInt()) % m_numAttribs)) == m_classIndex);
	}
	else {
	  r = m_random.nextInt() % m_numAttribs;
	}
	
	if (newPop[j].get(r)) {
	  newPop[j].clear(r);
	}
	else {
	  newPop[j].set(r);
	}
      }
      else {
	// crossover
	double r = m_random.nextDouble();
	if (r < m_pCrossover) {
	  // cross point
	  int cp = Math.abs(m_random.nextInt());
	  
	  cp %= (m_numAttribs-2);
	  cp ++;
	  
	  for (i=0;i<cp;i++) {
	    if (m_population[parent1].get(i)) {
	      newPop[j+1].set(i);
	    }
	    else {
	      newPop[j+1].clear(i);
	    }
	    if (m_population[parent2].get(i)) {
	      newPop[j].set(i);
	    }
	    else {
	      newPop[j].clear(i);
	    }
	  }
	}

	// mutate
	for (int k=0;k<2;k++) {
	  for (i=0;i<m_numAttribs;i++) {
	    r = m_random.nextDouble();
	    if (r < m_pMutation) {
	      if (m_hasClass && (i == m_classIndex)) {
		// ignore class attribute
	      }
	      else {
		if (newPop[j+k].get(i)) {
		  newPop[j+k].clear(i);
		}
		else {
		  newPop[j+k].set(i);
		}
	      }
	    }
	  }
	}
		  
      }
    }

    m_population = newPop;
  }

  /**
   * selects a population member to be considered for crossover
   * @return the index of the selected population member
   */
  private int select() {
    int i;
    double r,partsum;

    partsum = 0;
    r = m_random.nextDouble() * m_sumFitness;
    for (i=0;i<m_popSize;i++) {
      partsum += m_population[i].getFitness();
      if (partsum >= r) {
	break;
      }
    }
    return i;
  }

  /**
   * evaluates an entire population. Population members are looked up in
   * a hash table and if they are not found then they are evaluated using
   * ASEvaluator.
   * @param ASEvaluator the subset evaluator to use for evaluating population
   * members
   * @exception if something goes wrong during evaluation
   */
  private void evaluatePopulation (SubsetEvaluator ASEvaluator)
    throws Exception {
    int i;
    double merit;

    for (i=0;i<m_popSize;i++) {
      // if its not in the lookup table then evaluate and insert
      if (m_lookupTable.containsKey(m_population[i]
				    .getChromosome()) == false) {
	merit = ASEvaluator.evaluateSubset(m_population[i].getChromosome());
	m_population[i].setObjective(merit);
	m_lookupTable.put(m_population[i].getChromosome(),m_population[i]);
      } else {
	GABitSet temp = (GABitSet)m_lookupTable.
	  get(m_population[i].getChromosome());
	m_population[i].setObjective(temp.getObjective());
      }
    }
  }

  /**
   * creates random population members for the initial population. Also
   * sets the first population member to be a start set (if any) 
   * provided by the user
   * @exception if the population can't be created
   */
  private void initPopulation () throws Exception {
    int i,j,bit;
    int num_bits;
    boolean ok;
    int start = 0;

    // add the start set as the first population member (if specified)
    if (m_starting != null) {
      m_population[0] = new GABitSet();
      for (i=0;i<m_starting.length;i++) {
	if ((m_starting[i]) != m_classIndex) {
	  m_population[0].set(m_starting[i]);
	}
      }
      start = 1;
    }

    for (i=start;i<m_popSize;i++) {
      m_population[i] = new GABitSet();
      
      num_bits = m_random.nextInt();
      num_bits = num_bits % m_numAttribs-1;
      if (num_bits < 0) {
	num_bits *= -1;
      }
      if (num_bits == 0) {
	num_bits = 1;
      }

      for (j=0;j<num_bits;j++) {
	ok = false;
	do {
	  bit = m_random.nextInt();
	  if (bit < 0) {
	    bit *= -1;
	  }
	  bit = bit % m_numAttribs;
	  if (m_hasClass) {
	    if (bit != m_classIndex) {
	      ok = true;
	    }
	  }
	  else {
	    ok = true;
	  }
	} while (!ok);
	
	if (bit > m_numAttribs) {
	  throw new Exception("Problem in population init");
	}
	m_population[i].set(bit);
      }
    }
  }

  /**
   * calculates summary statistics for the current population
   */
  private void populationStatistics() {
    int i;
    
    m_sumFitness = m_minFitness = m_maxFitness = 
      m_population[0].getObjective();

    for (i=1;i<m_popSize;i++) {
      m_sumFitness += m_population[i].getObjective();
      if (m_population[i].getObjective() > m_maxFitness) {
	m_maxFitness = m_population[i].getObjective();
      }
      else if (m_population[i].getObjective() < m_minFitness) {
	m_minFitness = m_population[i].getObjective();
      }
    }
    m_avgFitness = (m_sumFitness / m_popSize);
  }

  /**
   * scales the raw (objective) merit of the population members
   */
  private void scalePopulation() {
    int j;
    double a = 0;
    double b = 0;
    double fmultiple = 2.0;
    double delta;
    
    // prescale
    if (m_minFitness > ((fmultiple * m_avgFitness - m_maxFitness) / 
			(fmultiple - 1.0))) {
      delta = m_maxFitness - m_avgFitness;
      a = ((fmultiple - 1.0) * m_avgFitness / delta);
      b = m_avgFitness * (m_maxFitness - fmultiple * m_avgFitness) / delta;
    }
    else {
      delta = m_avgFitness - m_minFitness;
      a = m_avgFitness / delta;
      b = -m_minFitness * m_avgFitness / delta;
    }
      
    // scalepop
    m_sumFitness = 0;
    for (j=0;j<m_popSize;j++) {
      m_population[j].
	setFitness(Math.abs((a * m_population[j].getObjective() + b)));
      m_sumFitness += m_population[j].getFitness();
    }
  }
  
  /**
   * generates a report on the current population
   * @return a report as a String
   */
  private String populationReport (int genNum) {
    int i;
    StringBuffer temp = new StringBuffer();

    if (genNum == 0) {
      temp.append("\nInitial population\n");
    }
    else {
      temp.append("\nGeneration: "+genNum+"\n");
    }
    temp.append("merit   \tscaled  \tsubset\n");
    
    for (i=0;i<m_popSize;i++) {
      temp.append(Utils.doubleToString(Math.
				       abs(m_population[i].getObjective()),
				       8,5)
		  +"\t"
		  +Utils.doubleToString(m_population[i].getFitness(),
					8,5)
		  +"\t");

      temp.append(printPopMember(m_population[i].getChromosome())+"\n");
    }
    return temp.toString();
  }

  /**
   * prints a population member as a series of attribute numbers
   * @param temp the chromosome of a population member
   * @return a population member as a String of attribute numbers
   */
  private String printPopMember(BitSet temp) {
    StringBuffer text = new StringBuffer();

    for (int j=0;j<m_numAttribs;j++) {
      if (temp.get(j)) {
        text.append((j+1)+" ");
      }
    }
    return text.toString();
  }

  /**
   * prints a population member's chromosome
   * @param temp the chromosome of a population member
   * @return a population member's chromosome as a String
   */
  private String printPopChrom(BitSet temp) {
    StringBuffer text = new StringBuffer();

    for (int j=0;j<m_numAttribs;j++) {
      if (temp.get(j)) {
	text.append("1");
      } else {
	text.append("0");
      }
    }
    return text.toString();
  }

  /**
   * reset to default values for options
   */
  private void resetOptions () {
    m_population = null;
    m_popSize = 20;
    m_lookupTableSize = 1001;
    m_pCrossover = 0.6;
    m_pMutation = 0.033;
    m_maxGenerations = 20;
    m_reportFrequency = m_maxGenerations;
    m_starting = null;
    m_seed = 1;
  }
}
