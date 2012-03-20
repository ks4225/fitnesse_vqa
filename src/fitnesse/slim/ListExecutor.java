// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slim;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.StringTokenizer;

import util.ListUtility;

/**
 * executes a list of SLIM statements, and returns a list of return values.
 */
public class ListExecutor {
  private StatementExecutorInterface executor;
  private NameTranslator methodNameTranslator;
  private boolean verbose;

  public ListExecutor(SlimFactory slimFactory) throws Exception {
    this(false, slimFactory);
  }

  protected ListExecutor(boolean verbose, SlimFactory slimFactory) throws Exception {
    this.verbose = verbose;
    this.executor = slimFactory.getStatementExecutor();
    this.methodNameTranslator = slimFactory.getMethodNameTranslator();
  }
  
  public List<Object> execute(List<Object> statements) {
    String message = "!1 Instructions";
    verboseMessage(message);

    List<Object> result = new ArrayList<Object>();
    for (Object statement : statements) {
      List<Object> statementList = ListUtility.uncheckedCast(Object.class, statement);
      verboseMessage(statementList + "\n");
      Object retVal = new Statement(statementList, methodNameTranslator).execute(executor);
      // start "after-market additions"
      if (retVal instanceof ArrayList<?>) { // TODO necessary?
        ArrayList<?> tmpRetVal = (ArrayList<?>)retVal;
        for (int i=0; i<tmpRetVal.size(); i++) {
          
          // try reflection when we encounter NO_METHOD_IN_CLASS
          if (tmpRetVal.get(i).toString().contains("NO_METHOD_IN_CLASS")) {
            
            // find the problematic method name
            String badMethod = "";
            StringTokenizer st = new StringTokenizer(tmpRetVal.get(i).toString());
            while (st.hasMoreTokens()) {
              String current = st.nextToken();
              // TODO use better comparison here
              if (current.equals("message:<<NO_METHOD_IN_CLASS")) {
                // strip argument count
                badMethod = st.nextToken();
                badMethod = badMethod.substring(0, badMethod.indexOf('['));
                break;
              }
            }
            
            // find/replace bad method with reflection
            ListIterator li = statementList.listIterator();
            Object previousItem = ""; // storage for previous item (we need to append after "reflection")
//            ArrayList<Object> args = new ArrayList<Object>();
            boolean found = false;
//            boolean arrayify = false;
            
            while (li.hasNext()) {
              Object item = li.next();
              // append after substitution
              if (found) {
//                if (arrayify) {
//                  args.add(previousItem);
//                } else {
                  li.set(previousItem);
//                }
//                arrayify = true;
              }
              if (((String)item).equals(badMethod)) {
                li.set("reflection");
                found = true;
              }
              previousItem = item;
            }
            
            if (found) {
//              if (arrayify) {
//                args.add(previousItem);
//                li.add(args);
//              } else {
                // in case we are calling a 0 argument method
              if (previousItem.equals(badMethod)) {
                li.add(previousItem);
                li.add(null);
              } else {
                li.add((String)previousItem);
              }
//              }
            }
            
            // re-call with new method
            new Statement(statementList, methodNameTranslator).execute(executor);
          }
        }
      }
      // end "after-market additions"
      verboseMessage(retVal);
      verboseMessage("------");
      result.add(retVal);
      
      if (executor.stopHasBeenRequested()) {
        executor.reset();
        return result;
      }
    }
    return result;
  }

  private void verboseMessage(Object message) {
    if (verbose) System.out.println(message);
  }
}
