using UnityEngine;

// Handle ASTServer in a MonoBehaviour
public class ServerSystem : MonoBehaviour
{
    public GameObject inputReceiver;

    ASTServer server;

    // Subscribe InputReceiver to ASTServer and start ASTServer
    void Start()
    {
        server = new ASTServer();
        server.Subscribe(101);
        inputReceiver.GetComponent<InputReceiver>().SetupData(101);
        server.Start();
    }

    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space))
        {
            server.Abort();
        }
        if (Input.GetKeyDown(KeyCode.S))
        {
            Debug.Log(server.AllThreadDown());
        }
    }

    private void OnApplicationQuit()
    {
        if (server != null)
            server.Abort();
    }
}
