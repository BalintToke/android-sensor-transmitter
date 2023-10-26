using UnityEngine;

public class BulletLogic : MonoBehaviour
{
    public float speed = 5f;
    public GameObject A;
    public GameObject B;
    public Vector3 direction = new Vector3(0, -1f, 0);

    void Start()
    {
        transform.position = A.transform.position;
        direction = B.transform.position - A.transform.position;
    }

    void Update()
    {
        transform.position += direction * Time.deltaTime * speed;
    }
}
