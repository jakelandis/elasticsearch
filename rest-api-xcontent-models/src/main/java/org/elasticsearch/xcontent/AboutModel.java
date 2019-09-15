package org.elasticsearch.xcontent;

import org.elasticsearch.common.xcontent.GenerateXContentModel;
import org.elasticsearch.common.xcontent.GenerateXContentModels;
import org.elasticsearch.common.xcontent.ToXContent;

@GenerateXContentModels({
    @GenerateXContentModel(model = "model/about.json", packageName = "org.elasticsearch.xcontent.generated.model", className = "AboutModelImpl"),
    @GenerateXContentModel(model = "model/about.v7.json", packageName = "org.elasticsearch.xcontent.generated.model", className = "AboutModelImplV7")
})
public interface AboutModel extends ToXContent {
}
