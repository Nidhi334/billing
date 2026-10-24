#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <Vision/Vision.h>
#import <AppKit/AppKit.h>

@interface CameraScanner : NSObject <AVCaptureVideoDataOutputSampleBufferDelegate>
@property (strong) AVCaptureSession *session;
@property (strong) dispatch_queue_t queue;
@property (strong) CIContext *ciContext;
@property (assign) int frameCounter;
@property (strong) NSString *lastBarcode;
@property (assign) NSTimeInterval lastBarcodeTime;
@end

@implementation CameraScanner
- (instancetype)init {
    self = [super init];
    if (self) {
        _queue = dispatch_queue_create("camera.queue", DISPATCH_QUEUE_SERIAL);
        _ciContext = [CIContext contextWithOptions:nil];
        _frameCounter = 0;
        _lastBarcode = @"";
        _lastBarcodeTime = 0;
    }
    return self;
}

- (BOOL)startCamera {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (status == AVAuthorizationStatusNotDetermined) {
        dispatch_semaphore_t sema = dispatch_semaphore_create(0);
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
            dispatch_semaphore_signal(sema);
        }];
        dispatch_semaphore_wait(sema, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(10 * NSEC_PER_SEC)));
    }

    AVCaptureDevice *device = nil;
    if (@available(macOS 10.15, *)) {
        NSMutableArray *types = [NSMutableArray arrayWithObject:AVCaptureDeviceTypeBuiltInWideAngleCamera];
        if (@available(macOS 14.0, *)) {
            [types addObject:AVCaptureDeviceTypeExternal];
            [types addObject:AVCaptureDeviceTypeContinuityCamera];
        }
        AVCaptureDeviceDiscoverySession *discovery = [AVCaptureDeviceDiscoverySession
            discoverySessionWithDeviceTypes:types
            mediaType:AVMediaTypeVideo
            position:AVCaptureDevicePositionUnspecified];
        if (discovery.devices.count > 0) {
            device = discovery.devices[0];
        }
    }
    if (!device) {
        device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    }

    if (!device) {
        fprintf(stderr, "NO_CAMERA_DEVICE\n");
        return NO;
    }

    NSError *error = nil;
    AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
    if (error || !input) {
        fprintf(stderr, "CANNOT_OPEN_DEVICE: %s\n", error ? [[error localizedDescription] UTF8String] : "unknown");
        return NO;
    }

    _session = [[AVCaptureSession alloc] init];
    if ([_session canSetSessionPreset:AVCaptureSessionPreset640x480]) {
        _session.sessionPreset = AVCaptureSessionPreset640x480;
    }

    if ([_session canAddInput:input]) {
        [_session addInput:input];
    }

    AVCaptureVideoDataOutput *output = [[AVCaptureVideoDataOutput alloc] init];
    output.alwaysDiscardsLateVideoFrames = YES;
    [output setSampleBufferDelegate:self queue:_queue];
    if ([_session canAddOutput:output]) {
        [_session addOutput:output];
    }

    [_session startRunning];
    printf("STARTED\n");
    fflush(stdout);
    return YES;
}

- (void)stopCamera {
    if (_session && _session.isRunning) {
        [_session stopRunning];
    }
    printf("STOPPED\n");
    fflush(stdout);
}

- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    if (!imageBuffer) return;

    // 1. Barcode Detection via Vision
    VNImageRequestHandler *handler = [[VNImageRequestHandler alloc] initWithCVPixelBuffer:imageBuffer options:@{}];
    VNDetectBarcodesRequest *barcodeRequest = [[VNDetectBarcodesRequest alloc] initWithCompletionHandler:^(VNRequest *req, NSError *err) {
        if (err) return;
        NSTimeInterval now = [NSDate timeIntervalSinceReferenceDate];
        for (VNBarcodeObservation *obs in req.results) {
            NSString *val = obs.payloadStringValue;
            if (val && val.length > 0) {
                // Rate limit identical barcode to once per 1.0s, but immediate for new barcode
                if (![val isEqualToString:self.lastBarcode] || (now - self.lastBarcodeTime > 1.0)) {
                    self.lastBarcode = val;
                    self.lastBarcodeTime = now;
                    printf("BARCODE:%s\n", [val UTF8String]);
                    fflush(stdout);
                }
            }
        }
    }];
    [handler performRequests:@[barcodeRequest] error:nil];

    // 2. Video Preview Frame (send 1 every 2 frames ~ 15 fps)
    _frameCounter++;
    if (_frameCounter % 2 == 0) {
        CIImage *ci = [CIImage imageWithCVPixelBuffer:imageBuffer];
        CGSize extent = ci.extent.size;
        if (extent.width > 420) {
            float s = 420.0f / extent.width;
            ci = [ci imageByApplyingTransform:CGAffineTransformMakeScale(s, s)];
        }
        CGImageRef cgImage = [_ciContext createCGImage:ci fromRect:[ci extent]];
        if (cgImage) {
            NSBitmapImageRep *rep = [[NSBitmapImageRep alloc] initWithCGImage:cgImage];
            NSData *jpeg = [rep representationUsingType:NSBitmapImageFileTypeJPEG properties:@{NSImageCompressionFactor: @0.5}];
            if (jpeg && jpeg.length > 0) {
                NSString *b64 = [jpeg base64EncodedStringWithOptions:0];
                printf("FRAME:%s\n", [b64 UTF8String]);
                fflush(stdout);
            }
            CGImageRelease(cgImage);
        }
    }
}
@end

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        CameraScanner *scanner = [[CameraScanner alloc] init];
        if (![scanner startCamera]) {
            return 1;
        }

        // Listen for STOP on stdin
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0), ^{
            char buf[64];
            while (fgets(buf, sizeof(buf), stdin)) {
                if (strncmp(buf, "STOP", 4) == 0) {
                    [scanner stopCamera];
                    exit(0);
                }
            }
            [scanner stopCamera];
            exit(0);
        });

        [[NSRunLoop currentRunLoop] run];
    }
    return 0;
}
