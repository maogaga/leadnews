package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;


    /**
     * 图片上传
     * @param multipartFile
     * @return
     */
    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        //1.检查参数
        if(multipartFile == null || multipartFile.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.上传图片到minIO中
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //aa.jpg
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片到MinIO中，fileId:{}",fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }

        //3.保存到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setType((short)0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);

        //4.返回结果

        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 素材列表查询
     * @param dto
     * @return
     */

    @Override
    public ResponseResult findList(WmMaterialDto dto) {

        //1.检查参数
        dto.checkParam();

        //2.分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //是否收藏
        if(dto.getIsCollection() != null && dto.getIsCollection() == 1){
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }

        //按照用户查询
        lambdaQueryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());

        //按照时间倒序
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);


        page = page(page,lambdaQueryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }


    /**
     * 删除图片
     * @param id
     * @return
     */
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private  WmMaterialMapper wmMaterialMapper;
    /**
     * 删除图片
     * @param id
     * @return
     */
    public ResponseResult delPicture(Integer id) {

        //1.根据素材id查询文章素材关系表，判断素材是否与文章有引用关系
        LambdaQueryWrapper<WmNewsMaterial> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WmNewsMaterial::getMaterialId, id);

        WmNewsMaterial wmNewsMaterial = wmNewsMaterialMapper.selectOne(lqw);

        //2.wmNewsMaterial不为空，禁止删除
        if(wmNewsMaterial != null ){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3..当前素材没有被文章引用，直接删除
        wmMaterialMapper.deleteById(id);



        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }



    /**
     * 图片收藏
     * @param id
     * @return
     */
    public ResponseResult idCollect(Integer id) {

        LambdaUpdateWrapper<WmMaterial> lambda = new LambdaUpdateWrapper<>();
        lambda.set(WmMaterial::getIsCollection, 1)//要修改的字段和值
                .eq(WmMaterial::getId, id);//可以修改的条件
        WmMaterial wmMaterial=wmMaterialMapper.selectOne(Wrappers.<WmMaterial>lambdaQuery().eq(WmMaterial::getId,id));
        wmMaterialMapper.update(wmMaterial,lambda);
  //      this.update(lambda);//提交

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult cancelCollect(Integer id) {
        LambdaUpdateWrapper<WmMaterial> lambda = new LambdaUpdateWrapper<>();
        lambda.set(WmMaterial::getIsCollection, 0)//要修改的字段和值
                .eq(WmMaterial::getId, id);//可以修改的条件
        WmMaterial wmMaterial=wmMaterialMapper.selectOne(Wrappers.<WmMaterial>lambdaQuery().eq(WmMaterial::getId,id));
        wmMaterialMapper.update(wmMaterial,lambda);
        //      this.update(lambda);//提交

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


}
