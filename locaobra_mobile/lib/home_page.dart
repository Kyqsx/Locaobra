import 'package:flutter/material.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() {
    return HomePageState();
  }
}

class HomePageState extends State<HomePage> {
  int counter = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.red,
        title: Text('Locaobra Mobile',),
      ),
      
      body: Center(
        child: GestureDetector(
          child: Text(
            'Contador: $counter',
            style: TextStyle(
              fontSize: 20),),
          onTap: () {
            setState(() {
              counter++;
            });
          },
        )),
        floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
           setState(() {
              counter++;
            });
        }),
    );
  }
}
