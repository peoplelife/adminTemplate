package com.admin.template.service.file;

import com.admin.template.common.exception.AppException;
import com.admin.template.common.exception.BizException;
import com.admin.template.common.exception.BusinessError;
import com.admin.template.common.exception.SystemError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mockStatic;

class FileServiceTest {
    @TempDir
    Path publicRoot;

    @Test
    void rejectsUnsupportedMimeTypes() {
        FileService service = new FileService(publicRoot);
        MockMultipartFile file = new MockMultipartFile("file", "payload.exe", "application/octet-stream",
                new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.upload(List.of(file), "uploads"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.VALIDATION_ERROR)
                .hasMessage("不支持的文件类型");
    }

    @Test
    void rejectsDeletionOutsideConfiguredPublicDirectory() {
        FileService service = new FileService(publicRoot);

        assertThatThrownBy(() -> service.deleteFile("../../secret.txt"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.VALIDATION_ERROR)
                .hasMessage("文件路径不合法");
    }

    @Test
    void acceptsAllowedMimeAndStoresInsideDefaultUploadDirectory() throws Exception {
        FileService service = new FileService(publicRoot);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        var uploaded = service.upload(List.of(file), null).getFirst();

        assertThat(uploaded.url()).startsWith("/public/uploads/").endsWith(".png");
        assertThat(publicRoot.resolve(uploaded.url().substring("/public/".length()))).exists();
        assertThatCode(() -> service.deleteFile(uploaded.url())).doesNotThrowAnyException();
        assertThat(publicRoot.resolve(uploaded.url().substring("/public/".length()))).doesNotExist();
    }

    @Test
    void rejectsMoreThanTenFilesBeforeWritingAnyFile() {
        FileService service = new FileService(publicRoot);
        List<MockMultipartFile> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> new MockMultipartFile("files", "photo.png", "image/png", new byte[]{1}))
                .toList();

        assertThatThrownBy(() -> service.upload(new java.util.ArrayList<>(files), "uploads"))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.VALIDATION_ERROR)
                .hasMessage("文件数量超过限制（最多10个）");
        assertThat(publicRoot.resolve("uploads")).doesNotExist();
    }

    @Test
    void rejectsMissingFilesWithTheSourceValidationContract() {
        FileService service = new FileService(publicRoot);

        assertThatThrownBy(() -> service.upload(List.of(), null))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.VALIDATION_ERROR)
                .hasMessage("请选择要上传的文件");
    }

    @Test
    void acceptsAnAllowedMimeTypeWithZeroBytes() {
        FileService service = new FileService(publicRoot);
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        var uploaded = service.upload(List.of(file), null).getFirst();

        assertThat(uploaded.size()).isZero();
        assertThat(publicRoot.resolve(uploaded.url().substring("/public/".length()))).exists().isEmptyFile();
    }

    @Test
    void rejectsFilesLargerThanTenMebibytesWithTheSourceValidationContract() {
        FileService service = new FileService(publicRoot);
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.upload(List.of(file), null))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("error", BusinessError.VALIDATION_ERROR)
                .hasMessage("文件大小超过限制（最大10MB）");
    }

    @Test
    void preservesOriginalUploadFailureForErrorLogging() {
        FileService service = new FileService(publicRoot);
        IOException failure = new IOException("simulated storage write failure");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1}) {
            @Override
            public void transferTo(Path destination) throws IOException {
                throw failure;
            }
        };

        assertThatThrownBy(() -> service.upload(List.of(file), "uploads"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", SystemError.FILE_UPLOAD_FAILED)
                .hasMessage("文件上传失败")
                .hasCause(failure);
    }

    @Test
    void preservesOriginalDeletionFailureForErrorLogging() throws Exception {
        FileService service = new FileService(publicRoot);
        Path target = Files.createFile(publicRoot.resolve("photo.png")).toRealPath();
        IOException failure = new IOException("simulated storage deletion failure");
        try (var files = mockStatic(Files.class)) {
            files.when(() -> Files.exists(target)).thenReturn(true);
            files.when(() -> Files.isRegularFile(target)).thenReturn(true);
            files.when(() -> Files.delete(target)).thenThrow(failure);

            assertThatThrownBy(() -> service.deleteFile("photo.png"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("error", SystemError.FILE_DELETE_FAILED)
                    .hasMessage("文件删除失败")
                    .hasCause(failure);
        }
    }
}
