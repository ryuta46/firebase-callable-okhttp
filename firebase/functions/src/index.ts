import * as functions from 'firebase-functions';

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//
export const helloWorld = functions.https.onRequest((request, response) => {
    response.send("Hello from Firebase!");
});


export const helloWorldOnCall = functions.https.onCall(async (data, context) => {
    const text = data.text || "";
    const uid = context.auth.uid;

    console.log("start sleep");
    await sleep(20000);
    console.log("end sleep");

    return {
        message: "Hello World from onCall.",
        uid: uid,
        text: text
    };
});


function sleep(milliseconds: number) {
    return new Promise<void>(resolve => {
        setTimeout(() => resolve(), milliseconds);
    });
}

