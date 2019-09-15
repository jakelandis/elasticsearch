package org.elasticsearch.xcontent;

import org.elasticsearch.common.xcontent.GenerateXContentModel;
import org.elasticsearch.common.xcontent.GenerateXContentModels;


@GenerateXContentModels({
    @GenerateXContentModel(model = "model/about.json", packageName = "org.elasticsearch.xcontent", className = "AboutModelImpl1"),
    @GenerateXContentModel(model = "model/about.v7.json", packageName = "org.elasticsearch.xcontent", className = "AboutModelImplv7")
})
public interface AboutModel {
}
