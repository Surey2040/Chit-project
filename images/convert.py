import os
import glob
from xml.etree import ElementTree as ET

svgs = glob.glob('*.svg')
for svg_file in svgs:
    tree = ET.parse(svg_file)
    root = tree.getroot()
    path_data = root.find('{http://www.w3.org/2000/svg}path').attrib['d']
    
    xml_content = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960">
  <group android:translateY="960">
    <path
        android:fillColor="#FF000000"
        android:pathData="{path_data}"/>
  </group>
</vector>"""
    
    name = svg_file.split('_24dp')[0] + '.xml'
    name = 'ic_' + name
    out_path = os.path.join(r'd:\Chits\android-app\app\src\main\res\drawable', name)
    
    # Ensure directory exists
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    
    with open(out_path, 'w') as f:
        f.write(xml_content)
    print('Created', out_path)
