const puppeteer = require("puppeteer");
const cas = require("../../cas.js");

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await cas.gotoLogin(page);

    await cas.loginWith(page);
    await cas.assertTextContent(page, "#content h1", "Authentication Interrupt");
    await cas.assertTextContentStartsWith(page, "#content p", "The authentication flow has been interrupted");
    await cas.assertTextContent(page, "#interruptMessage", "We interrupted your login");
    await cas.assertCookie(page);
    await cas.assertVisibility(page, "#interruptLinks");
    await page.waitForTimeout(1000);
    await cas.click(page, "#casapplication");
    await page.waitForNavigation();
    await cas.assertTicketParameter(page);
    await browser.close();
})();
