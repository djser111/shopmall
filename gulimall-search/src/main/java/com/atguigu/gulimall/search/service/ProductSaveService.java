package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
