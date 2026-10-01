/**
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

#import "CDVWKInAppBrowserUIDelegate.h"

// scheme://host[:port] without the default port, like window.location.origin
static NSString *CDVWKInAppBrowserOrigin(NSString *scheme, NSString *host, NSInteger port)
{
    if (scheme.length == 0 || host.length == 0) {
        return @"";
    }
    NSString *lowerScheme = scheme.lowercaseString;
    BOOL defaultPort = port <= 0 ||
        ([lowerScheme isEqualToString:@"https"] && port == 443) ||
        ([lowerScheme isEqualToString:@"http"] && port == 80);
    if (defaultPort) {
        return [NSString stringWithFormat:@"%@://%@", lowerScheme, host.lowercaseString];
    }
    return [NSString stringWithFormat:@"%@://%@:%ld", lowerScheme, host.lowercaseString, (long)port];
}

@implementation CDVWKInAppBrowserUIDelegate

+ (NSArray<NSString *> *)originsFromList:(NSString *)list
{
    NSMutableArray<NSString *> *origins = [NSMutableArray array];
    for (NSString *entry in [list componentsSeparatedByString:@"|"]) {
        NSString *trimmed = [entry stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceCharacterSet];
        NSURLComponents *url = [NSURLComponents componentsWithString:trimmed];
        NSString *origin = CDVWKInAppBrowserOrigin(url.scheme, url.host, url.port.integerValue);
        if ([origin hasPrefix:@"https://"]) {
            [origins addObject:origin];
        }
    }
    return origins;
}

+ (NSString *)originOf:(WKSecurityOrigin *)origin
{
    return origin == nil ? @"" : CDVWKInAppBrowserOrigin(origin.protocol, origin.host, origin.port);
}

// OutSystems fork: camera and microphone only for pages on a permissionorigins origin, and
// WebKit then asks the user. Before iOS 15 there is no such hook and WebKit always asks.
- (void)webView:(WKWebView *)webView requestMediaCapturePermissionForOrigin:(WKSecurityOrigin *)origin
                                                          initiatedByFrame:(WKFrameInfo *)frame
                                                                      type:(WKMediaCaptureType)type
                                                           decisionHandler:(void (^)(WKPermissionDecision decision))decisionHandler API_AVAILABLE(ios(15.0))
{
    NSString *pageOrigin = CDVWKInAppBrowserOrigin(origin.protocol, origin.host, origin.port);
    BOOL allowed = pageOrigin.length > 0 && (self.permissionOrigins == nil || [self.permissionOrigins containsObject:pageOrigin]);
    decisionHandler(allowed ? WKPermissionDecisionPrompt : WKPermissionDecisionDeny);
}

- (instancetype)initWithTitle:(NSString *)title
{
    self = [super init];
    if (self) {
        self.title = title;
    }

    return self;
}

- (void)webView:(WKWebView *)webView runJavaScriptAlertPanelWithMessage:(NSString *)message
initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(void))completionHandler
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:self.title
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

    UIAlertAction *ok = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK")
                                                 style:UIAlertActionStyleDefault
                                               handler:^(UIAlertAction *action)
        {
            completionHandler();
            [alert dismissViewControllerAnimated:YES completion:nil];
        }];

    [alert addAction:ok];

    [[self getViewController] presentViewController:alert animated:YES completion:nil];
}

- (void)webView:(WKWebView *)webView runJavaScriptConfirmPanelWithMessage:(NSString *)message
initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(BOOL result))completionHandler
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:self.title
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

    UIAlertAction *ok = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK")
                                                 style:UIAlertActionStyleDefault
                                               handler:^(UIAlertAction *action)
        {
            completionHandler(YES);
            [alert dismissViewControllerAnimated:YES completion:nil];
        }];

    [alert addAction:ok];

    UIAlertAction *cancel = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"Cancel")
                                                     style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction *action)
        {
            completionHandler(NO);
            [alert dismissViewControllerAnimated:YES completion:nil];
        }];
    [alert addAction:cancel];

    [[self getViewController] presentViewController:alert animated:YES completion:nil];
}

- (void)webView:(WKWebView *)webView runJavaScriptTextInputPanelWithPrompt:(NSString *)prompt
defaultText:(NSString *)defaultText initiatedByFrame:(WKFrameInfo *)frame
completionHandler:(void (^)(NSString *result))completionHandler
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:self.title
                                                                   message:prompt
                                                            preferredStyle:UIAlertControllerStyleAlert];

    UIAlertAction *ok = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK", @"OK")
                                                 style:UIAlertActionStyleDefault
                                               handler:^(UIAlertAction *action)
        {
            completionHandler(((UITextField *)alert.textFields[0]).text);
            [alert dismissViewControllerAnimated:YES completion:nil];
        }];

    [alert addAction:ok];

    UIAlertAction *cancel = [UIAlertAction actionWithTitle:NSLocalizedString(@"Cancel", @"Cancel")
                                                     style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction *action)
        {
            completionHandler(nil);
            [alert dismissViewControllerAnimated:YES completion:nil];
        }];
    [alert addAction:cancel];

    [alert addTextFieldWithConfigurationHandler:^(UITextField *textField) {
        textField.text = defaultText;
    }];

    [[self getViewController] presentViewController:alert animated:YES completion:nil];
}

- (UIViewController *)getViewController
{
    return _viewController;
}

- (void)setViewController:(UIViewController *)viewController
{
    _viewController = viewController;
}

@end
