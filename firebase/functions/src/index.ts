import * as functions from 'firebase-functions';

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//
export const helloWorld = functions.https.onRequest((request, response) => {
    response.send("Hello from Firebase!");
});


export const helloWorldOnCall = functions.https.onCall((data, context) => {
    const text = data.text;
    const uid = context.auth.uid;

    return {
        message: "Hello World from onCall.",
        uid: uid,
        text: text
    };
});
