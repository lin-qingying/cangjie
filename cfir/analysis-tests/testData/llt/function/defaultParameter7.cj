// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output
 
class CustomView {}
 
class ObservedProperty<T> {}
 
class VideoTrimmerView {
    public init(parent: Option<CustomView>, 
        videoTrimmerOption!: ObservedProperty<Float64>, 
        hintText!: Option<ObservedProperty<Float64>>= None, 
        videoPlayIcon!: ObservedProperty<Float64>, 
        videoPauseIcon!: ObservedProperty<Float64>, 
        videoThumbHandleIcon!: ObservedProperty<Float64>, 
        mLeftProgressPos!: Option<ObservedProperty<Float64>>= None, 
        mRightProgressPos!: Option<ObservedProperty<Float64>>= None, 
        mRedProgressBarPos!: Option<ObservedProperty<Float64>>= None, 
        mVideoParentHeight!: Option<ObservedProperty<Float64>>= None, 
        mVideoParentWidth!: Option<ObservedProperty<Float64>>= None, 
        mSeekBarWidth!: Option<ObservedProperty<Float64>>= None, 
        mSeekBarHeight!: Option<ObservedProperty<Float64>>= None, 
        mDuration!: Option<ObservedProperty<Float64>>= None, 
        isFromRestore!: Option<ObservedProperty<Bool>>= None, 
        scroller!: Option<ObservedProperty<Bool>>= None, 
        ratio!: Option<ObservedProperty<Float64>>= None, 
        currentTime!: Option<ObservedProperty<String>>= None, 
        durationTimeText!: Option<ObservedProperty<String>>= None, 
        setValue!: Option<ObservedProperty<Float64>>= None, 
        isLoop!: Option<ObservedProperty<Bool>>= None, 
        videoName!: Option<ObservedProperty<String>>= None, 
        isSpeed!: Option<ObservedProperty<Bool>>= None, 
        descriptionValue!: Option<ObservedProperty<String>>= None, 
        isBuffering!: Option<ObservedProperty<Bool>>= None, 
        speedTag!: Option<ObservedProperty<Int64>>= None, 
        scaleTag!: Option<ObservedProperty<Int64>>= None, 
        avPlayer!: Option<ObservedProperty<?Float64>>= None, 
        surfaceID!: Option<ObservedProperty<String>>= None, 
        mXcomponentController!: Option<ObservedProperty<Float64>>= None, 
        isLoaded!: Option<ObservedProperty<Bool>>= None, 
        isTrimming!: Option<ObservedProperty<Bool>>= None, 
        VIDEO_MAX_TIME!: Option<ObservedProperty<Float64>>= None, 
        VIDEO_MIN_TIME!: Option<ObservedProperty<Float64>>= None, 
        MAX_COUNT_RANGE!: Option<ObservedProperty<Float64>>= None, 
        THUMB_WIDTH!: Option<ObservedProperty<Float64>>= None, 
        PAD_LINE_WIDTH!: Option<ObservedProperty<Float64>>= None, 
        mVideoThumbOption!: Option<ObservedProperty<Float64>>= None, 
        mRangSeekBarOption!: Option<ObservedProperty<Float64>>= None, 
        mMediaPlayerStatus!: Option<ObservedProperty<Float64>>= None, 
        startTruncationTime!: Option<ObservedProperty<Float64>>= None, 
        endTruncationTime!: Option<ObservedProperty<Float64>>= None, 
        videoThumbWidth!: Option<ObservedProperty<Int32>>= None, 
        videoThumbHeight!: Option<ObservedProperty<Int32>>= None, 
        px2vpvalue!: Option<ObservedProperty<Float64>>= None, 
        seekBarCallBack!: Option<ObservedProperty<Option<Float64>>>= None, 
        isNext!: Option<ObservedProperty<Bool>>= None, 
        onReadNext!: Option<ObservedProperty <() -> Unit>>= None, 
        fdHead!: Option<ObservedProperty<String>>= None, 
        fdPath!: Option<ObservedProperty<String>>= None, 
        fd!: Option<ObservedProperty<Int32>>= None, 
        isPausePreview!: Option<ObservedProperty<Bool>>= None, 
        globalContext!: Option<ObservedProperty<Option<Float64>>>= None, 
        localStorage!: Option<Float64>= None) {}
}
 
main() {
    VideoTrimmerView(CustomView(), videoTrimmerOption: ObservedProperty<Float64>(), globalContext: ObservedProperty<Option<Float64>>(), videoPlayIcon: ObservedProperty<Float64>(), videoPauseIcon: ObservedProperty<Float64>(), videoThumbHandleIcon: ObservedProperty<Float64>())
    0
}
