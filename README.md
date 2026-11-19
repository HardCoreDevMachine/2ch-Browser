## 2ch Browser

2ch is an imageboard where anyone can communicate easily and freely, where any point of view is acceptable. 
The existing boards are dedicated to a variety of topics such as music, videogames, technologies, and comics. 
There is no registration and you don't have to write your name to participate in the community. 
Select any board according to your interests and jump right in.

<img alt="List of threads" src="https://lh3.ggpht.com/l1trjGygFflcWpGirBNn_5jqTSmyT_2jwgHUbnHtNa2iB2c4jrKDWoNBPXDGFoduz7A" width="200" />
<img alt="List of threads, black theme" src="https://lh4.ggpht.com/WqHdGasXUdYFBvy_g0PD900Uzl-bTJzX0Uwmw15Ti7MM2fOsYfRcFPeUC1VHY_B6wg" width="200" />
<img alt="List of posts" src="https://lh6.ggpht.com/gikWpKVmQK_vrWDWyqGwmU5c3DK_Hgd9p_YmIN6BsQAMmscLSOOCHBrPnVyQUMQJ9uc" width="200" />
<img alt="List of tabs" src="https://lh5.ggpht.com/SAfqIvfvia1GewTu_MDiQpsVO0RWEhYnFg8WNamhjvIQs4JL4-JL491Yy0phd58OdMc2" width="200" />
<img alt="Adding a new post" src="https://lh6.ggpht.com/Sz5T-wP8v3DpObPUVIBg76qtyrKsS_sXzXxd28JY-Uz2-TXBQy10bKnlL6d82KKj-g" width="200" />

Direct link:
https://dl.dropboxusercontent.com/u/75518900/2ch-Browser-release.apk

## Building Source Code

Eclipse: 
* File → Import → Android → Existing Android code into workspace → select the whole folder.
* EclipseCleanUpProfile.xml - Window → Preferences → Java → Code Style → Clean Up → Import → select the file → Apply.
* EclipseFormatterProfile.xml - Window → Preferences → Java → Code Style → Formatter → Import → select the file → Apply.

IntelliJ IDEA / Android Studio: 
* Import Project → select the settings.gradle file.



## Posting acknowledgement

Posting follows the sanitized successful `2ch.su.har` request: `/user/posting?nc=1`,
`task=post`, AJAX JSON headers, `captcha_type=emoji_captcha`, `emoji_captcha_id`
and the `2ch_challenge` nonce. Captcha requests stay on their original origin;
changing the configured domain requires refreshing the captcha before posting.
The existing HTTP client and its cookie store are retained.

The response must acknowledge the requested operation with `result: 1` and a
positive `num` (reply) or `thread` (new thread). The client retains those IDs and
performs one uncached `/api/mobile/v2/post/{board}/{num}` read-back. Only a matching
board, post number and parent confirms visibility. Failed read-back does not
erase the acknowledgement or resend the post: the UI offers to open the thread.
After returning to the thread, a full uncached reload targets the created post.

POST requests are not automatically retried. A lost response is an unknown
outcome, not proof that no post was created. API errors use the server message,
or the `api.yml` ErrorCode description when that message is missing. Non-success
dialogs can copy protocol diagnostics without adb: endpoint, status, result,
error codes and post/thread IDs, never comments, files, cookies or captcha tokens.

Regression coverage includes parser/error mapping and `PostSenderIntegrationTest`,
which exercises the production sender against a local TCP HTTP fixture, including
lost responses, redirects, attachments and read-back failures. The HAR-derived
fixture in `app/src/test/resources/posting/har-success.json` contains no live tokens.
Local protocol tests do not substitute for an on-device publication/read-back check.

## License

[MIT License][license]

[license]: https://github.com/vortexwolf/2ch-Browser/blob/master/MIT-LICENSE.txt


