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
 *    Classifier.java
 *    Copyright (C) 1999 Eibe Frank, Len Trigg
 *
 */

package weka.classifiers;

import java.io.Serializable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.SerializedObject;
import weka.core.Utils;


/** 
 * Abstract classifier. All schemes for numeric or nominal prediction in
 * Weka extend this class. Note that a classifier MUST either implement
 * distributionForInstance() or classifyInstance().
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $
 */
public abstract class Classifier implements Cloneable, Serializable {
 
  /**
   * Generates a classifier. Must initialize all fields of the classifier
   * that are not being set via options (ie. multiple calls of buildClassifier
   * must always lead to the same result). Must not change the dataset
   * in any way.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the classifier has not been 
   * generated successfully
   */
  public abstract void buildClassifier(Instances data) throws Exception;

  /**
   * Classifies the given test instance. The instance has to belong to a
   * dataset when it's being classified. Note that a classifier MUST
   * implement either this or distributionForInstance().
   *
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or 
   * Instance.missingValue() if no prediction is made
   * @exception Exception if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {

    double [] dist = distributionForInstance(instance);
    if (dist == null) {
      throw new Exception("Null distribution predicted");
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      double max = 0;
      int maxIndex = 0;
      
      for (int i = 0; i < dist.length; i++) {
	if (dist[i] > max) {
	  maxIndex = i;
	  max = dist[i];
	}
      }
      if (max > 0) {
	return maxIndex;
      } else {
	return Instance.missingValue();
      }
    case Attribute.NUMERIC:
      return dist[0];
    default:
      return Instance.missingValue();
    }
  }

  /**
   * Predicts the class memberships for a given instance. If
   * an instance is unclassified, the returned array elements
   * must be all zero. If the class is numeric, the array
   * must consist of only one element, which contains the
   * predicted value. Note that a classifier MUST implement
   * either this or classifyInstance().
   *
   * @param instance the instance to be classified
   * @return an array containing the estimated membership 
   * probabilities of the test instance in each class 
   * or the numeric prediction
   * @exception Exception if distribution could not be 
   * computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] dist = new double[instance.numClasses()];
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      dist[(int)classifyInstance(instance)] = 1.0;
      return dist;
    case Attribute.NUMERIC:
      dist[0] = classifyInstance(instance);
      return dist;
    default:
      return dist;
    }
  }    
  
  /**
   * Creates a new instance of a classifier given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * classifier implements OptionHandler and the options parameter is
   * non-null, the classifier will have it's options set.
   *
   * @param classifierName the fully qualified class name of the classifier
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created classifier, ready for use.
   * @exception Exception if the classifier name is invalid, or the options
   * supplied are not acceptable to the classifier
   */
  public static Classifier forName(String classifierName,
				   String [] options) throws Exception {

    return (Classifier)Utils.forName(Classifier.class,
				     classifierName,
				     options);
  }

  /**
   * Creates copies of the current classifier, which can then
   * be used for boosting etc. Note that this method now uses
   * Serialization to perform a deep copy, so the Classifier
   * object must be fully Serializable. Any currently built model
   * will now be copied as well.
   *
   * @param model an example classifier to copy
   * @param num the number of classifiers copies to create.
   * @return an array of classifiers.
   * @exception Exception if an error occurs
   */
  public static Classifier [] makeCopies(Classifier model,
					 int num) throws Exception {

    if (model == null) {
      throw new Exception("No model classifier set");
    }
    Classifier [] classifiers = new Classifier [num];
    SerializedObject so = new SerializedObject(model);
    for(int i = 0; i < classifiers.length; i++) {
      classifiers[i] = (Classifier) so.getObject();
    }
    return classifiers;
  }
}
