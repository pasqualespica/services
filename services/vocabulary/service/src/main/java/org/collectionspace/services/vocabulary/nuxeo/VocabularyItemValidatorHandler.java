/**
 *  This document is a part of the source code and related artifacts
 *  for CollectionSpace, an open source collections management system
 *  for museums and related institutions:

 *  http://www.collectionspace.org
 *  http://wiki.collectionspace.org

 *  Copyright 2009 University of California at Berkeley

 *  Licensed under the Educational Community License (ECL), Version 2.0.
 *  You may not use this file except in compliance with this License.

 *  You may obtain a copy of the ECL 2.0 License at

 *  https://source.collectionspace.org/collection-space/LICENSE.txt

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.collectionspace.services.vocabulary.nuxeo;

import java.util.regex.Pattern;

import org.collectionspace.services.relation.RelationsCommonList;
import org.collectionspace.services.vocabulary.VocabularyitemsCommon;
import org.collectionspace.services.client.RelationClient;
import org.collectionspace.services.common.context.MultipartServiceContext;
import org.collectionspace.services.common.context.ServiceContext;
import org.collectionspace.services.common.document.DocumentHandler.Action;
import org.collectionspace.services.common.document.InvalidDocumentException;
import org.collectionspace.services.common.document.ValidatorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VocabularyItemValidatorHandler
 * 
 * Validates data supplied when attempting to create and/or update VocabularyItem records.
 * 
 * $LastChangedRevision: $
 * $LastChangedDate: $
 */
public class VocabularyItemValidatorHandler implements ValidatorHandler {

    final Logger logger = LoggerFactory.getLogger(VocabularyItemValidatorHandler.class);
    private static final Pattern shortIdBadPattern = Pattern.compile("[\\W]"); //.matcher(input).matches()

    @Override
    public void validate(Action action, ServiceContext ctx)
            throws InvalidDocumentException {
        if (logger.isDebugEnabled()) {
            logger.debug("validate() action=" + action.name());
        }
        
        // Bail out if the validation action is for delete.
        if (action.equals(Action.DELETE)) {
        	return;
        }
        
        try {
            String errMessage = "";
            boolean invalid = false;
            MultipartServiceContext mctx = (MultipartServiceContext) ctx;
            VocabularyitemsCommon vocabItem = (VocabularyitemsCommon) mctx.getInputPart(mctx.getCommonPartLabel());
            
            if (vocabItem != null) {
	            // Validation occurring on both creates and updates
	            String displayName = vocabItem.getDisplayName();
	            if (displayName == null || displayName.trim().length() < 2) {
	                invalid = true;
	                errMessage += "displayName must be non-null and contain at least 2 non-whitespace characters";
	            }
	            
	            // Validation specific to creates or updates
	            if (action.equals(Action.CREATE)) {
	                String shortId = vocabItem.getShortIdentifier();
	                // Per CSPACE-2215, shortIdentifier values that are null (missing
	                // or the empty string) are now legally accepted in CREATE/POST payloads.
	                // In either of these cases, a short identifier will be synthesized from
	                // a display name or supplied in another manner.
	                if ((shortId != null) && (shortIdBadPattern.matcher(shortId).find())) {
	                    invalid = true;
	                    errMessage += "shortIdentifier must only contain standard word characters";
	                }
	            } else if (action.equals(Action.UPDATE)) {
	            	// What is this ELSE clause for?
	            }
            } else {
            	RelationsCommonList rcl = (RelationsCommonList) mctx.getInputPart(RelationClient.SERVICE_COMMON_LIST_NAME);
            	if (rcl == null) {
                    invalid = true;
                    errMessage += "The vocabulary item payload is missing both a common payload and relations part.  At lease one of these must exist in the payload.";
            	}            	
            }
            
            if (invalid) {
                logger.error(errMessage);
                throw new InvalidDocumentException(errMessage);
            }
        } catch (InvalidDocumentException ide) {
            throw ide;
        } catch (Exception e) {
            throw new InvalidDocumentException(e);
        }
    }
}
