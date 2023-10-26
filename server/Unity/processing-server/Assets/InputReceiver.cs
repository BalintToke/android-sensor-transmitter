using UnityEngine;

public class InputReceiver : MonoBehaviour
{
    public float forceSpeed = 5f;

    public GameObject activeBullets;
    public GameObject bullett;
    GameObject A;
    GameObject B;

    private int inputId = -1;
    bool subed = false;

    bool currentlyShooting = false;

    void Start()
    {
        A = transform.GetChild(0).gameObject;
        B = transform.GetChild(1).gameObject;
    }

    public void SetupData(int id)
    {
        inputId = id;
        ASTServer.onReceivedValue += ProcessData;
        subed = true;
    }

    void Update()
    {
        if (Input.GetKeyDown(KeyCode.O))
        {
            if (subed)
                ASTServer.onReceivedValue -= ProcessData;
            else
                ASTServer.onReceivedValue += ProcessData;
        }
        if (currentlyShooting)
        {
            Shoot();
            currentlyShooting = false;
        }
    }

    void Shoot()
    {
        GameObject newBullett= Instantiate(bullett, activeBullets.transform);

        BulletLogic bl = newBullett.GetComponent<BulletLogic>();
        bl.A = A;
        bl.B = B;
        
        newBullett.SetActive(true);
    }

    void ProcessData(int id, Quaternion eulerAng, Vector3 force,bool shooting)
    {
        if (id != inputId)
            return;

        // Delta calculations
        //transform.localRotation = Quaternion.RotateTowards(transform.localRotation, transform.localRotation * eulerAng, 20f);

        // Direct rotation application
        transform.localRotation = Quaternion.RotateTowards(transform.localRotation,eulerAng,20f);

        if (shooting)
        {
            currentlyShooting = shooting;
        }

    }

    public void OnDestroy()
    {
        ASTServer.onReceivedValue -= ProcessData;
    }
}
