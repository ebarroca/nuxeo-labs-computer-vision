/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.vision.core.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.labs.vision.core.test.mock.MockWorkManager;
import org.nuxeo.labs.vision.core.worker.VideoVisionWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.labs.nuxeo-labs-computer-vision-core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.automation.scripting"
})
@LocalDeploy({
        "org.nuxeo.labs.nuxeo-labs-google-vision-core:OSGI-INF/mock-work-manager-contrib.xml"
})
public class TestVideoEventChain {

    @Inject
    CoreSession session;

    @Inject
    protected TagService tagService;


    @Test
    public void testVideoChain() throws IOException, OperationException {

        DocumentModel video = session.createDocumentModel("/", "Video", "Video");
        File file = new File(getClass().getResource("/files/plane2.jpg").getPath());
        Blob blob = new FileBlob(file);
        Map<String,Serializable> storyboardItem = new HashMap<>();
        storyboardItem.put("comment","mytitle");
        storyboardItem.put("content", (Serializable) blob);
        List<Map<String,Serializable>> storyboard = new ArrayList<>();
        storyboard.add(storyboardItem);
        video.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, (Serializable) storyboard);
        video = session.createDocument(video);

        AutomationService as = Framework.getService(AutomationService.class);
        OperationContext ctx = new OperationContext();
        ctx.setInput(video);
        ctx.setCoreSession(session);
        OperationChain chain = new OperationChain("TestVideoChain");
        chain.add("javascript.VideoVisionDefaultMapper");
        video = (DocumentModel) as.run(ctx, chain);

        List<Tag> tags =
                tagService.getDocumentTags(session,video.getId(),session.getPrincipal().getName());

        Assert.assertTrue(tags.size()>0);
        System.out.print(tags);
    }


    @Test
    public void testVideoWorker() throws IOException, OperationException {

        DocumentModel video = session.createDocumentModel("/", "Video", "Video");
        video = session.createDocument(video);

        File file = new File(getClass().getResource("/files/plane2.jpg").getPath());
        Blob blob = new FileBlob(file);
        Map<String,Serializable> storyboardItem = new HashMap<>();
        storyboardItem.put("comment","mytitle");
        storyboardItem.put("content", (Serializable) blob);
        List<Map<String,Serializable>> storyboard = new ArrayList<>();
        storyboard.add(storyboardItem);
        video.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, (Serializable) storyboard);

        MockWorkManager wm = (MockWorkManager)Framework.getService(WorkManager.class);
        wm.isActive = false;
        video = session.saveDocument(video);

        VideoVisionWorker work = new VideoVisionWorker(video.getRepositoryName(),video.getId());
        work.work();
        Assert.assertEquals("Done",work.getStatus());
        work.cleanUp(true,null);

    }


    @Test
    public void testVideoListener() throws IOException, OperationException {

        DocumentModel video = session.createDocumentModel("/", "Video", "Video");
        video = session.createDocument(video);

        File file = new File(getClass().getResource("/files/plane2.jpg").getPath());
        Blob blob = new FileBlob(file);
        Map<String,Serializable> storyboardItem = new HashMap<>();
        storyboardItem.put("comment","mytitle");
        storyboardItem.put("content", (Serializable) blob);
        List<Map<String,Serializable>> storyboard = new ArrayList<>();
        storyboard.add(storyboardItem);
        video.setPropertyValue(VideoConstants.STORYBOARD_PROPERTY, (Serializable) storyboard);

        MockWorkManager wm = (MockWorkManager)Framework.getService(WorkManager.class);
        wm.isActive = true;
        wm.wasSchedule = false;

        video = session.saveDocument(video);

        Assert.assertTrue(wm.wasSchedule);
    }

}
