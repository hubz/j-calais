/*
 *  Copyright 2010 BigData Mx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package mx.bigdata.jcalais;

import java.io.IOException;
import java.util.Collection;

public class CalaisException extends IOException {
  
  private final String method;
  
  private final String calaisRequestID;
  
  private final String creationDate;
  
  private final String calaisVersion;

  private final String message;

  public CalaisException(String method, String calaisRequestID, 
                         String creationDate, String calaisVersion, 
                         String message) {
    this.method = method;
    this.calaisRequestID = calaisRequestID;
    this.creationDate = creationDate;
    this.calaisVersion = calaisVersion;
    this.message = message;
  }
  
  public String getMethod() {
    return method;
  }
  
  public String getCalaisRequestID() {
    return calaisRequestID;
  }
  
  public String getCreationDate() {
    return creationDate;
  }
  
  public String getCalaisVersion() {
    return calaisVersion;
  }

  public String getMessage() {
    return message;
  }

  public String getCalaisMessage() {
    return String.format("Method=%s\ncalaisRequestID=%s\n"
                         + "CreationDate=%s\nCalaisVersion=%s\n"
                         + "Exception:\n\t%s", method, calaisRequestID, 
                         creationDate, calaisVersion, message);
  }
}