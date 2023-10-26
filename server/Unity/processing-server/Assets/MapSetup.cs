using UnityEngine;

// Create a wall of cubes for the player to shoot at
public class MapSetup : MonoBehaviour
{
    public GameObject target;

    private void Awake()
    {
        for (int i = 0; i < 90; i++)
        {
            for (int j = 0; j < 30; j++)
            {
                GameObject cTarget = Instantiate(target, transform);
                cTarget.transform.localPosition = new Vector3(i * 0.3f, j * -0.3f, 0);
                cTarget.SetActive(true);
            }
        }
    }
}
