package liuyuyang.net.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.qiniu.common.QiniuException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import liuyuyang.net.annotation.CheckRole;
import liuyuyang.net.execption.CustomException;
import liuyuyang.net.result.Result;
import liuyuyang.net.utils.OssUtil;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.get.RemoteDirInfo;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * 统一文件上传
 *
 * @author laifeng
 * @date 2024/12/14
 */
@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
@Transactional
@CheckRole
public class FileController {
    @Resource
    private FileStorageService fileStorageService;

    @PostMapping
    @ApiOperation("文件上传")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 1)
    public Result<Object> add(@RequestParam(defaultValue = "") String dir, @RequestParam MultipartFile[] files) throws IOException {
        if (dir == null || dir.trim().isEmpty()) throw new CustomException(400, "请指定一个目录");

        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            FileInfo result = fileStorageService.of(file)
                    .setPlatform(OssUtil.getPlatform())
                    .setPath(dir + '/')
                    .upload();

            if (result == null) throw new CustomException("上传文件失败");
            urls.add(result.getUrl());
        }

        return Result.success("文件上传成功：", urls);
    }

    @DeleteMapping
    @ApiOperation("删除文件")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 2)
    public Result<String> del(@RequestParam String filePath) {
        boolean delete = fileStorageService.delete(filePath);
        System.out.println(delete);
        return Result.status(delete);
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除文件")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 3)
    public Result batchDel(@RequestBody String[] pathList) throws QiniuException {
        for (String url : pathList) {
            boolean delete = fileStorageService.delete(url);
            if (!delete) throw new CustomException("删除文件失败");
        }
        return Result.success();
    }

    @GetMapping("/info")
    @ApiOperation("获取文件信息")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 4)
    public Result<FileInfo> get(@RequestParam String filePath) throws QiniuException {
        FileInfo fileInfo = fileStorageService.getFileInfoByUrl(filePath);
        return Result.success(fileInfo);
    }

    @GetMapping("/dir")
    @ApiOperation("获取目录列表")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 5)
    public Result<List<Map>> getDirList() {
        ListFilesResult result = fileStorageService.listFiles()
                .setPlatform(OssUtil.getPlatform())
                .listFiles();

        // 获取文件列表
        List<Map> list = new ArrayList<>();
        List<RemoteDirInfo> fileList = result.getDirList();

        for (RemoteDirInfo item : fileList) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", item.getName());
            data.put("path", item.getOriginal());
            list.add(data);
        }

        return Result.success(list);
    }

    @GetMapping("/list")
    @ApiOperation("获取指定目录中的文件")
    @ApiOperationSupport(author = "刘宇阳 | liuyuyang1024@yeah.net", order = 5)
    public Result<List<Map>> getFileList(@RequestParam String dir) {
        if (dir == null || dir.trim().isEmpty()) throw new CustomException(400, "请指定一个目录");

        ListFilesResult result = fileStorageService.listFiles()
                .setPlatform(OssUtil.getPlatform())
                .setPath(dir + '/')
                .listFiles();

        // 获取文件列表
        List<Map> list = new ArrayList<>();
        List<RemoteFileInfo> fileList = result.getFileList();

        for (RemoteFileInfo item : fileList) {
            // 如果是目录就略过
            if (Objects.equals(item.getExt(), "")) continue;

            Map<String, Object> data = new HashMap<>();

            data.put("basePath", item.getBasePath());
            data.put("dir", dir);
            data.put("path", item.getBasePath() + item.getPath() + item.getFilename());
            data.put("name", item.getFilename());
            data.put("size", item.getSize());
            data.put("type", item.getExt());
            data.put("url", item.getUrl());

            list.add(data);
        }

        return Result.success(list);
    }
}
