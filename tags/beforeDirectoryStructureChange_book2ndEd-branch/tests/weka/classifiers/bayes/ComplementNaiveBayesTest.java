/*
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

/*
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.bayes;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ComplementNaiveBayes. Run from the command line with:<p/>
 * java weka.classifiers.bayes.ComplementNaiveBayes
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.3 $
 */
public class ComplementNaiveBayesTest 
  extends AbstractClassifierTest {

  public ComplementNaiveBayesTest(String name) { 
    super(name);  
  }

  /** Creates a default ComplementNaiveBayes */
  public Classifier getClassifier() {
    return new ComplementNaiveBayes();
  }

  /**
   * Returns a fully configured tester instance.
   * 
   * @return		the configured tester
   */
  protected CheckClassifier getTester() {
    CheckClassifier	result;
    
    result = super.getTester();
    result.setPostProcessor(new AbsPostProcessor());
    
    return result;
  }

  public static Test suite() {
    return new TestSuite(ComplementNaiveBayesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}