/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 *(Link.builder().regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless(Link.builder().required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.vcloud.director.v1_5.features;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.isEmpty;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.GETTER_RETURNS_SAME_OBJ;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_DEL;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_ATTRB_DEL;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_CLONE;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_CONTAINS;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_EQ;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_LIST_SIZE_EQ;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_LIST_SIZE_GE;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_REQ;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_REQ_LIVE;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_FIELD_UPDATABLE;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.OBJ_REQ_LIVE;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.TASK_COMPLETE_TIMELY;
import static org.jclouds.vcloud.director.v1_5.predicates.LinkPredicates.relEquals;
import static org.jclouds.vcloud.director.v1_5.predicates.LinkPredicates.typeEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import org.jclouds.io.Payloads;
import org.jclouds.vcloud.director.v1_5.VCloudDirectorMediaType;
import org.jclouds.vcloud.director.v1_5.domain.Checks;
import org.jclouds.vcloud.director.v1_5.domain.File;
import org.jclouds.vcloud.director.v1_5.domain.Link;
import org.jclouds.vcloud.director.v1_5.domain.Media;
import org.jclouds.vcloud.director.v1_5.domain.Metadata;
import org.jclouds.vcloud.director.v1_5.domain.MetadataEntry;
import org.jclouds.vcloud.director.v1_5.domain.MetadataValue;
import org.jclouds.vcloud.director.v1_5.domain.Owner;
import org.jclouds.vcloud.director.v1_5.domain.Reference;
import org.jclouds.vcloud.director.v1_5.domain.Task;
import org.jclouds.vcloud.director.v1_5.domain.Vdc;
import org.jclouds.vcloud.director.v1_5.domain.params.CloneMediaParams;
import org.jclouds.vcloud.director.v1_5.internal.BaseVCloudDirectorApiLiveTest;
import org.jclouds.vcloud.director.v1_5.predicates.LinkPredicates;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Tests behavior of {@code MediaApi}
 * 
 * @author danikov
 */
@Test(groups = { "live", "user" }, singleThreaded = true, testName = "MediaApiLiveTest")
public class MediaApiLiveTest extends BaseVCloudDirectorApiLiveTest {

   public static final String MEDIA = "media";
   public static final String VDC = "vdc";

   /*
    * Convenience references to API apis.
    */
   protected VdcApi vdcApi;
   protected MediaApi mediaApi;
   
   /*
    * Shared state between dependent tests.
    */
   private Media media, oldMedia;
   private Owner owner;
   private Metadata metadata;
   private MetadataValue metadataValue;
   private String metadataEntryValue = "value";

   @Override
   @BeforeClass(alwaysRun = true)
   public void setupRequiredApis() {
      vdcApi = context.getApi().getVdcApi();
      mediaApi = context.getApi().getMediaApi();
   }
   
   @AfterClass(alwaysRun = true)
   protected void tidyUp() {
      if (media != null) {
         try {
	         Task remove = mediaApi.removeMedia(media.getHref());
	         taskDoneEventually(remove);
         } catch (Exception e) {
            logger.warn(e, "Error when deleting media '%s': %s", media.getName());
         }
      }
      if (oldMedia != null) {
         try {
	         Task remove = mediaApi.removeMedia(oldMedia.getHref());
	         taskDoneEventually(remove);
         } catch (Exception e) {
            logger.warn(e, "Error when deleting media '%s': %s", oldMedia.getName());
         }
      }
   }
   
   @Test(description = "POST /vdc/{id}/media")
   public void testAddMedia() throws URISyntaxException {
      Vdc vdc = lazyGetVdc(); 
      Link addMedia = find(vdc.getLinks(), and(relEquals("add"), typeEquals(VCloudDirectorMediaType.MEDIA)));
      
      // TODO: generate an iso
      byte[] iso = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
      
      Media sourceMedia = Media.builder()
            .type(VCloudDirectorMediaType.MEDIA)
            .name("Test media "+random.nextInt())
            .size(iso.length)
            .imageType(Media.ImageType.ISO)
            .description("Test media generated by testAddMedia()")
            .build();
      media = mediaApi.addMedia(addMedia.getHref(), sourceMedia);
      
      Checks.checkMediaFor(MEDIA, media);
      
      assertNotNull(media.getFiles(), String.format(OBJ_FIELD_REQ, MEDIA, "files"));
      assertTrue(media.getFiles().size() == 1, String.format(OBJ_FIELD_LIST_SIZE_EQ, MEDIA, "files", 1, media.getFiles().size()));
      File uploadFile = getFirst(media.getFiles(), null);
      assertNotNull(uploadFile, String.format(OBJ_FIELD_REQ, MEDIA, "files.first"));
      assertEquals(uploadFile.getSize(), Long.valueOf(iso.length));
      assertEquals(uploadFile.getSize().longValue(), sourceMedia.getSize(),
            String.format(OBJ_FIELD_EQ, MEDIA, "uploadFile.size()", sourceMedia.getSize(), uploadFile.getSize()));
      
      Set<Link> links = uploadFile.getLinks();
      assertNotNull(links, String.format(OBJ_FIELD_REQ, MEDIA, "uploadFile.links"));
      assertTrue(links.size() >= 1, String.format(OBJ_FIELD_LIST_SIZE_GE, MEDIA, "uploadfile.links", 1, links.size()));
      assertTrue(Iterables.all(links, Predicates.or(LinkPredicates.relEquals(Link.Rel.UPLOAD_DEFAULT), LinkPredicates.relEquals(Link.Rel.UPLOAD_ALTERNATE))),
            String.format(OBJ_FIELD_REQ, MEDIA, "uploadFile.links.first"));

      Link uploadLink = Iterables.find(links, LinkPredicates.relEquals(Link.Rel.UPLOAD_DEFAULT));
      context.getApi().getUploadApi().upload(uploadLink.getHref(), Payloads.newByteArrayPayload(iso));
      
      media = mediaApi.getMedia(media.getHref());
      if (media.getTasks().size() == 1) {
         Task uploadTask = Iterables.getOnlyElement(media.getTasks());
         Checks.checkTask(uploadTask);
         assertEquals(uploadTask.getStatus(), Task.Status.RUNNING);
         assertTrue(retryTaskSuccess.apply(uploadTask), String.format(TASK_COMPLETE_TIMELY, "uploadTask"));
         media = mediaApi.getMedia(media.getHref());
      }
   }
   
   @Test(description = "GET /media/{id}", dependsOnMethods = { "testAddMedia" })
   public void testGetMedia() {
      media = mediaApi.getMedia(media.getHref());
      assertNotNull(media, String.format(OBJ_REQ_LIVE, MEDIA));
      
      owner = media.getOwner();
      assertNotNull(owner, String.format(OBJ_FIELD_REQ_LIVE, MEDIA, "owner"));
      Checks.checkResourceType(media.getOwner());
      
      Checks.checkMediaFor(MEDIA, media);
   }
   
   @Test(description = "GET /media/{id}/owner", dependsOnMethods = { "testGetMedia" })
   public void testGetMediaOwner() {
      Owner directOwner = mediaApi.getOwner(media.getHref());
      assertEquals(owner.toBuilder()
            .user(owner.getUser())
            .build(),
         directOwner.toBuilder().links(Collections.<Link>emptySet()).build(),
         String.format(GETTER_RETURNS_SAME_OBJ,
         "getOwner()", "owner", "media.getOwner()", owner.toString(), directOwner.toString()));
      
      // parent type
      Checks.checkResourceType(directOwner);
      
      // required
      assertNotNull(directOwner.getUser(), String.format(OBJ_FIELD_REQ, "Owner", "user"));
      Checks.checkReferenceType(directOwner.getUser());
   }
   
   @Test(description = "POST /vdc/{id}/action/cloneMedia", dependsOnMethods = { "testGetMediaOwner" })
   public void testCloneMedia() {
      oldMedia = media;
      media = vdcApi.cloneMedia(vdcUrn, CloneMediaParams.builder()
            .source(Reference.builder().fromEntity(media).build())
            .name("copied "+media.getName())
            .description("copied by testCloneMedia()")
            .build());
      
      Checks.checkMediaFor(VDC, media);
      
      if (media.getTasks() != null) {
         Task copyTask = getFirst(media.getTasks(), null);
         if (copyTask != null) {
            Checks.checkTask(copyTask);
            assertTrue(retryTaskSuccess.apply(copyTask), String.format(TASK_COMPLETE_TIMELY, "copyTask"));
            media = mediaApi.getMedia(media.getHref());
         }
      }
      
      Checks.checkMediaFor(MEDIA, media);
      assertTrue(media.clone(oldMedia), String.format(OBJ_FIELD_CLONE, MEDIA, "copied media", 
            media.toString(), oldMedia.toString()));
      
      mediaApi.getMetadataApi().putEntry(media.getHref(), "key", MetadataValue.builder().value("value").build());
      
      media = vdcApi.cloneMedia(vdcUrn, CloneMediaParams.builder()
            .source(Reference.builder().fromEntity(media).build())
            .name("moved test media")
            .description("moved by testCloneMedia()")
            .isSourceDelete(true)
            .build());
      
      Checks.checkMediaFor(VDC, media);
      
      if (media.getTasks() != null) {
         Task copyTask = getFirst(media.getTasks(), null);
         if (copyTask != null) {
            Checks.checkTask(copyTask);
            assertTrue(retryTaskSuccess.apply(copyTask), String.format(TASK_COMPLETE_TIMELY, "copyTask"));
            media = mediaApi.getMedia(media.getHref());
         }
      }
      
      Checks.checkMediaFor(MEDIA, media);
      assertTrue(media.clone(oldMedia), String.format(OBJ_FIELD_CLONE, MEDIA, "moved media", 
            media.toString(), oldMedia.toString()));
   }
   
   @Test(description = "PUT /media/{id}", dependsOnMethods = { "testCloneMedia" })
   public void testSetMedia() {
      String oldName = media.getName();
      String newName = "new "+oldName;
      String oldDescription = media.getDescription();
      String newDescription = "new "+oldDescription;
      media = media.toBuilder().name(newName).description(newDescription).build();
      
      Task editMedia = mediaApi.editMedia(media.getHref(), media);
      Checks.checkTask(editMedia);
      assertTrue(retryTaskSuccess.apply(editMedia), String.format(TASK_COMPLETE_TIMELY, "editMedia"));
      media = mediaApi.getMedia(media.getHref());
      
      assertTrue(equal(media.getName(), newName), String.format(OBJ_FIELD_UPDATABLE, MEDIA, "name"));
      assertTrue(equal(media.getDescription(), newDescription),
            String.format(OBJ_FIELD_UPDATABLE, MEDIA, "description"));
      
      //TODO negative tests?
      
      Checks.checkMediaFor(MEDIA, media);
      
      media = media.toBuilder().name(oldName).description(oldDescription).build();
      
      editMedia = mediaApi.editMedia(media.getHref(), media);
      Checks.checkTask(editMedia);
      assertTrue(retryTaskSuccess.apply(editMedia), String.format(TASK_COMPLETE_TIMELY, "editMedia"));
      media = mediaApi.getMedia(media.getHref());
   }
   
   @Test(description = "GET /media/{id}/metadata", dependsOnMethods = { "testSetMetadataValue" })
   public void testGetMetadata() {
      metadata = mediaApi.getMetadataApi().get(media.getHref());
      // required for testing
      assertFalse(isEmpty(metadata.getMetadataEntries()),
            String.format(OBJ_FIELD_REQ_LIVE, MEDIA, "metadata.entries"));
      
      Checks.checkMetadataFor(MEDIA, metadata);
   }
   
   @Test(description = "POST /media/{id}/metadata", dependsOnMethods = { "testGetMedia" })
   public void testMergeMetadata() {
      // test new
      Set<MetadataEntry> inputEntries = ImmutableSet.of(MetadataEntry.builder().entry("testKey", "testValue").build());
      Metadata inputMetadata = Metadata.builder()
            .entries(inputEntries)
            .build();
      
      Task mergeMetadata = mediaApi.getMetadataApi().merge(media.getHref(), inputMetadata);
      Checks.checkTask(mergeMetadata);
      assertTrue(retryTaskSuccess.apply(mergeMetadata), String.format(TASK_COMPLETE_TIMELY, "mergeMetadata(new)"));
      metadata = mediaApi.getMetadataApi().get(media.getHref());
      Checks.checkMetadataFor(MEDIA, metadata);
      checkMetadataContainsEntries(metadata, inputEntries);
      
      media = mediaApi.getMedia(media.getHref());
      Checks.checkMediaFor(MEDIA, media);
      
      // test edit
      inputEntries = ImmutableSet.of(MetadataEntry.builder().entry("testKey", "new testValue").build());
      inputMetadata = Metadata.builder()
            .entries(inputEntries)
            .build();
      
      mergeMetadata = mediaApi.getMetadataApi().merge(media.getHref(), inputMetadata);
      Checks.checkTask(mergeMetadata);
      assertTrue(retryTaskSuccess.apply(mergeMetadata), String.format(TASK_COMPLETE_TIMELY, "mergeMetadata(edit)"));
      metadata = mediaApi.getMetadataApi().get(media.getHref());
      Checks.checkMetadataFor(MEDIA, metadata);
      checkMetadataContainsEntries(metadata, inputEntries);
      
      media = mediaApi.getMedia(media.getHref());
      Checks.checkMediaFor(MEDIA, media);
   }
   
   private void checkMetadataContainsEntries(Metadata metadata, Set<MetadataEntry> entries) {
      for (MetadataEntry inputEntry : entries) {
         boolean found = false;
         for (MetadataEntry entry : metadata.getMetadataEntries()) {
            if (equal(inputEntry.getKey(), entry.getKey())) {
               found = true; break;
            }
         }
         
         if (!found) {
            String.format(OBJ_FIELD_CONTAINS, MEDIA, "metadata",
                  Iterables.toString(metadata.getMetadataEntries()),
                  Iterables.toString(entries));
         }
      }
   }
   
   @Test(description = "GET /media/{id}/metadata/{key}", dependsOnMethods = { "testSetMetadataValue" })
   public void testGetMetadataValue() {
      metadataValue = mediaApi.getMetadataApi().getValue(media.getHref(), "key");
      Checks.checkMetadataValueFor(MEDIA, metadataValue);
   }
   
   @Test(description = "PUT /media/{id}/metadata/{key}", dependsOnMethods = { "testMergeMetadata" })
   public void testSetMetadataValue() {
      metadataEntryValue = "value";
      MetadataValue newValue = MetadataValue.builder().value(metadataEntryValue).build();
      
      Task setMetadataEntry = mediaApi.getMetadataApi().putEntry(media.getHref(), "key", newValue);
      Checks.checkTask(setMetadataEntry);
      assertTrue(retryTaskSuccess.apply(setMetadataEntry),
            String.format(TASK_COMPLETE_TIMELY, "setMetadataEntry"));
      metadataValue = mediaApi.getMetadataApi().getValue(media.getHref(), "key");
      Checks.checkMetadataValueFor(MEDIA, metadataValue);
   }
   
   @Test(description = "DELETE /media/{id}/metadata/{key}", dependsOnMethods = { "testGetMetadata", "testGetMetadataValue" } )
   public void testRemoveMetadata() {
      Task removeEntry = mediaApi.getMetadataApi().removeEntry(media.getHref(), "testKey");
      Checks.checkTask(removeEntry);
      assertTrue(retryTaskSuccess.apply(removeEntry),
            String.format(TASK_COMPLETE_TIMELY, "removeEntry"));
      
      metadataValue = mediaApi.getMetadataApi().getValue(media.getHref(), "testKey");
      assertNull(metadataValue, String.format(OBJ_FIELD_ATTRB_DEL, MEDIA,
               "Metadata", metadataValue != null ? metadataValue.toString() : "",
               "MetadataEntry", metadataValue != null ? metadataValue.toString() : ""));
      
      metadataValue = mediaApi.getMetadataApi().getValue(media.getHref(), "key");
      Checks.checkMetadataValueFor(MEDIA, metadataValue);
      
      media = mediaApi.getMedia(media.getHref());
      Checks.checkMediaFor(MEDIA, media);
   }
   
   @Test(description = "DELETE /media/{id}", dependsOnMethods = { "testRemoveMetadata" } )
   public void testRemoveMedia() {
      Task removeMedia = mediaApi.removeMedia(media.getHref());
      Checks.checkTask(removeMedia);
      assertTrue(retryTaskSuccess.apply(removeMedia),
            String.format(TASK_COMPLETE_TIMELY, "removeMedia"));
      
      media = mediaApi.getMedia(media.getHref());
      assertNull(media, String.format(OBJ_DEL, MEDIA, media != null ? media.toString() : ""));
      
      removeMedia = mediaApi.removeMedia(oldMedia.getHref());
      Checks.checkTask(removeMedia);
      assertTrue(retryTaskSuccess.apply(removeMedia), String.format(TASK_COMPLETE_TIMELY, "removeMedia"));
      oldMedia = null;
   }
}
