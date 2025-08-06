package top.fifthlight.blazerod.extension.internal.gl;

import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.gl.CompiledComputePipeline;
import top.fifthlight.blazerod.pipeline.ComputePipeline;

public interface GpuDeviceExtInternal extends GpuDeviceExt {
    CompiledComputePipeline blazerod$compilePipelineCached(ComputePipeline pipeline);
}
