let ws;

function newRoom(){
    // calling the ChatServlet to retrieve a new room ID
    document.getElementById("log").value = ""; //clears chat log

    let callURL= "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet"; //chat servlet url to be called for code

    let roomListDiv = document.getElementById("room-list"); //gets the room list div

    fetch(callURL, { //fetch response
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => {
            enterRoom(response); //Enter the room

            const button = document.createElement('button'); //create a button
            button.innerHTML = response; //Set the innerHTML to the code
            button.style.display = "inline-block"; //set display type

            button.onclick = function() { //function for when the button is clicked
                enterRoom(response) //Enter the appropriate room
                document.getElementById("log").value = ""; //Clear the chat log
            };
            roomListDiv.appendChild(button); //Append the button to the div
        }); // enter the room with the code
}

function enterRoom(code){
    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/"+code);

    // parse messages received from the server and update the UI accordingly
    ws.onmessage = function (event) {
        // parsing the server's message as json
        let message = JSON.parse(event.data);

        // handle message
        if (message.type === "chat") { //extract the chat portion from the server
            document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n"; //display the chat in the log
        } else if (message.type === "title"){ //extract the title message from the server
            document.getElementById("title").value = message.message; //display the title of the chat log
        }
    }
}

document.getElementById("input").addEventListener("keyup", function (event) {
    if (event.keyCode === 13) {
        let request = {"type":"chat", "msg":event.target.value};
        ws.send(JSON.stringify(request));
        event.target.value = "";
    }
});

function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

function refresh() {
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/");
    let roomListDiv = document.getElementById("room-list"); //gets the room list div
    // parse messages received from the server and update the UI accordingly
    ws.onmessage = function (event) {
        // parsing the server's message as json
        let message = JSON.parse(event.data);

        const availableRooms = message.message; //Gets all the available rooms in the response
        availableRooms.filter(response => typeof response === "string" && response !== "") //Filters out any unnecessary rooms that may have been created
            .forEach(response => { //For each room...
                    let roomListDiv = document.getElementById("room-list"); //gets the room list div
                    const button = document.createElement('button'); //create a button
                    button.textContent = response; //Set the innerHTML to the code
                    button.style.display = "inline-block"; //set display type

                    button.onclick = function() { //function for when the button is clicked
                        enterRoom(response) //Enter the appropriate room
                    };
                    roomListDiv.appendChild(button); //Append the button to the div
                }
            );

    }
}
