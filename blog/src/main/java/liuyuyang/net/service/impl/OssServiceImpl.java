package liuyuyang.net.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import liuyuyang.net.enums.EOssPlatform;
import liuyuyang.net.execption.CustomException;
import liuyuyang.net.mapper.OssMapper;
import liuyuyang.net.model.Oss;
import liuyuyang.net.service.OssService;
import liuyuyang.net.utils.OssUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OssServiceImpl extends ServiceImpl<OssMapper, Oss> implements OssService {
    @Resource
    private OssMapper ossMapper;

    @Override
    public void saveOss(Oss oss) {
        // 判断是否有重复
        Integer count = this.lambdaQuery().eq(Oss::getPlatform, oss.getPlatform()).count();
        if (count > 0) throw new CustomException("该平台已存在，请勿重复添加");
        this.save(oss);
    }

    @Override
    public void delOss(Integer id) {
        Oss oss = this.getById(id);
        if (oss == null) throw new CustomException("删除失败");
        // 如果是默认的平台，提示不可删除
        if (oss.getPlatform().equals(EOssPlatform.LOCAL_PLUS.getValue())) throw new CustomException("默认平台不可删除");
        boolean result = this.removeById(id);
        if (result) OssUtil.removeStorage(OssUtil.getStorageList(), oss.getPlatform());
    }

    @Override
    public List<Oss> list() {
        QueryWrapper<Oss> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByDesc(Oss::getId);
        List<Oss> list = ossMapper.selectList(queryWrapper);

        for (Oss data : list) {
            data.setAccessKey(maskMiddleTen(data.getAccessKey()));
            data.setSecretKey(maskMiddleTen(data.getSecretKey()));
            data.setPlatformName(platformName(data.getPlatform()));
        }

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Integer id) {
        // 先禁用所有的配置
        boolean temp1 = this.update(Wrappers.<Oss>update().lambda().set(Oss::getIsEnable, 0));
        if (!temp1) throw new CustomException("操作失败");

        // 再启用制定的配置
        boolean temp2 = this.update(Wrappers.<Oss>update().lambda().set(Oss::getIsEnable, 1).eq(Oss::getId, id));
        if (!temp2) throw new CustomException("启用失败");

        Oss oss = this.getById(id);
        OssUtil.registerPlatform(oss);
    }

    public Oss getEnableOss() {
        QueryWrapper<Oss> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Oss::getIsEnable, 1);
        return ossMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Map> getPlatform() {
        List<Map> result = new ArrayList<>();
        String[] list = {"huawei", "aliyun", "qiniu", "tencent", "minio"};

        for (String item : list) {
            Map<String, String> data = new HashMap<>();
            data.put("name", platformName(item));
            data.put("value", item);
            result.add(data);
        }

        return result;
    }

    @Override
    public void updateOss(Oss oss) {
        String platform = oss.getPlatform();
        if (EOssPlatform.LOCAL_PLUS.getValue().equals(platform)) {
            // domin 后缀必须是 /static/
            if (!oss.getDomain().endsWith("/static/")) {
                throw new CustomException("本地存储域名必须以 /static/ 结尾");
            }
        }
        // 不允许更改平台
        oss.setPlatform(null);
        boolean result = this.updateById(oss);
        if (result) {
            oss.setPlatform(platform);
            OssUtil.registerPlatform(oss);
        }
    }

    // 对数据中间10位数进行脱敏
    public String maskMiddleTen(String input) {
        if (input == null || input.length() <= 10) return input;

        int start = (input.length() - 10) / 2;
        int end = start + 10;

        StringBuilder masked = new StringBuilder(input);
        for (int i = start; i < end; i++) {
            masked.setCharAt(i, '*');
        }

        return masked.toString();
    }

    // 处理平台名称
    public String platformName(String data) {
        switch (data) {
            case "local":
                return "本地存储";
            case "huawei":
                return "华为云";
            case "aliyun":
                return "阿里云";
            case "qiniu":
                return "七牛云";
            case "tencent":
                return "腾讯云";
            case "minio":
                return "Minio";
        }

        return data;
    }
}
