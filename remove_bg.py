from PIL import Image

def remove_white_bg(input_path, output_path):
    img = Image.open(input_path)
    img = img.convert("RGBA")
    datas = img.getdata()

    newData = []
    # If the pixel is close to white, make it transparent
    for item in datas:
        # Check if R, G, B are all high (white or near white)
        if item[0] > 200 and item[1] > 200 and item[2] > 200:
            newData.append((255, 255, 255, 0))
        else:
            newData.append(item)

    img.putdata(newData)
    img.save(output_path, "PNG")

remove_white_bg('d:\\Chits\\IMG_5153.PNG', 'd:\\Chits\\android-app\\app\\src\\main\\res\\drawable\\logo_jv.png')
print("Image background removed and saved to drawable!")
