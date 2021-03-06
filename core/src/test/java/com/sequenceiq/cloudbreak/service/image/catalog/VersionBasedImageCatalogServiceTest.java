package com.sequenceiq.cloudbreak.service.image.catalog;


import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VersionBasedImageCatalogServiceTest {

    private static final String CURRENT_CB_VERSION = "2.16";

    private static final String OTHER_CB_VERSION = "2.17";

    private static final String PROPER_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String PROPER_IMAGE_ID_2 = "36cbacf7-f7d4-4875-61f9-548a0acd3512";

    private static final String OTHER_IMAGE_ID = "6edcb9d4-4110-44d8-43f7-d4c0008402a3";

    @Mock
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private PrefixMatcherService prefixMatcherService;

    @Mock
    private CloudbreakImageCatalogV3 imageCatalogV3;

    @Mock
    private Images images;

    @Mock
    private Versions versions;

    @InjectMocks
    private VersionBasedImageCatalogService victim;

    @BeforeEach
    public void initTests() {
        when(imageCatalogV3.getImages()).thenReturn(images);
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Versions versions = createVersions();
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);
        when(images.getCdhImages()).thenReturn(List.of(properImage, otherImage));
        when(imageCatalogV3.getVersions()).thenReturn(versions);

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getAvailableImages().getCdhImages();

        assertTrue(actual.contains(properImage));
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsEmptyListWhenThereAreNoSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", "2.18");
        Versions versions = createVersions();
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.18"), any()))
                .thenReturn(new PrefixMatchImages(Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        when(images.getCdhImages()).thenReturn(List.of(properImage, otherImage));
        when(imageCatalogV3.getVersions()).thenReturn(versions);

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getAvailableImages().getCdhImages();

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreMultipleSupportedImagesAreAvailableForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Versions versions = createVersions();
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(images.getCdhImages()).thenReturn(List.of(properImage1, otherImage, properImage2));
        when(imageCatalogV3.getVersions()).thenReturn(versions);

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getAvailableImages().getCdhImages();

        assertTrue(actual.contains(properImage1));
        assertTrue(actual.contains(properImage2));
        assertEquals(2, actual.size());
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, true);
    }

    private Versions createVersions() {
        return new Versions(List.of(
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID)),
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID_2)),
                new CloudbreakVersion(List.of(OTHER_CB_VERSION), Collections.emptyList(), List.of(OTHER_IMAGE_ID))));
    }
}